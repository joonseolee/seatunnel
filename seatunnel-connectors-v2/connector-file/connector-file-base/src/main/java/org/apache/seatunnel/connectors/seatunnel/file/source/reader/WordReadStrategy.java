/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.connectors.seatunnel.file.source.reader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.seatunnel.api.source.Collector;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.connectors.seatunnel.file.config.FileFormat;
import org.apache.seatunnel.connectors.seatunnel.file.exception.FileConnectorException;

import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.shade.com.typesafe.config.Config;

@Slf4j
public class WordReadStrategy extends AbstractReadStrategy {

  private static final int DEFAULT_PAGE_NUMBER = 1;
  private static final int DEFAULT_POSITION = 1;

  private static class NodeInfo {
    String elementId;
    String parentId;
    List<String> childIds = new ArrayList<>();
    int positionIndex;

    NodeInfo(String elementId, String parentId, int positionIndex) {
      this.elementId = elementId;
      this.parentId = parentId;
      this.positionIndex = positionIndex;
    }
  }

  @Override
  public void read(String path, String tableId, Collector<SeaTunnelRow> output)
      throws IOException, FileConnectorException {
    Map<String, String> partitionsMap = parsePartitionsByPath(path);
    resolveArchiveCompressedInputStream(path, tableId, output, partitionsMap, FileFormat.WORD);
  }

  @Override
  protected void readProcess(
      String path,
      String tableId,
      Collector<SeaTunnelRow> output,
      InputStream inputStream,
      Map<String, String> partitionsMap,
      String currentFileName)
      throws IOException {

    try (XWPFDocument document = new XWPFDocument(inputStream)) {
      Map<Object, NodeInfo> nodeInfoMap = new IdentityHashMap<>();
      Map<String, Integer> typeCounters = new HashMap<>();
      List<SeaTunnelRow> rows = new ArrayList<>();

      // Word 문서의 모든 요소들을 순회하며 ID 할당 및 트리 구조 생성
      assignIdsAndCollectTree(document, null, nodeInfoMap, DEFAULT_POSITION, typeCounters);

      // 행 데이터 생성
      generateRows(document, rows, nodeInfoMap, DEFAULT_PAGE_NUMBER);

      // 출력
      for (SeaTunnelRow row : rows) {
        output.collect(row);
      }
    }
  }

  private void assignIdsAndCollectTree(
      Object node,
      Object parent,
      Map<Object, NodeInfo> nodeInfoMap,
      int position,
      Map<String, Integer> typeCounters) {

    String elementType = getElementType(node);
    String elementId = null;

    if (isEligibleForRow(node)) {
      int count = typeCounters.getOrDefault(elementType, 0) + 1;
      typeCounters.put(elementType, count);
      elementId = elementType + "_" + count;
    }

    String parentId = parent == null ? null : nodeInfoMap.get(parent).elementId;
    NodeInfo nodeInfo = new NodeInfo(elementId, parentId, position);
    nodeInfoMap.put(node, nodeInfo);

    // 자식 요소들 처리
    List<Object> children = getChildren(node);
    int childPosition = 1;
    for (Object child : children) {
      assignIdsAndCollectTree(child, node, nodeInfoMap, childPosition++, typeCounters);
      NodeInfo childInfo = nodeInfoMap.get(child);
      if (childInfo.elementId != null) {
        nodeInfo.childIds.add(childInfo.elementId);
      }
    }
  }

  private void generateRows(
      Object node, List<SeaTunnelRow> rows, Map<Object, NodeInfo> nodeInfoMap, int pageNumber) {
    if (isEligibleForRow(node)) {
      NodeInfo nodeInfo = nodeInfoMap.get(node);
      String elementType = getElementType(node);
      Integer headingLevel = null;
      String text = extractValue(node);

      if (node instanceof XWPFParagraph) {
        XWPFParagraph paragraph = (XWPFParagraph) node;
        if (paragraph.getStyle() != null && paragraph.getStyle().startsWith("Heading")) {
          try {
            headingLevel = Integer.parseInt(paragraph.getStyle().substring(7));
          } catch (NumberFormatException e) {
            // 스타일에서 레벨을 추출할 수 없는 경우
          }
        }
      }

      rows.add(
          new SeaTunnelRow(
              new Object[] {
                  nodeInfo.elementId,
                  elementType,
                  headingLevel,
                  text,
                  pageNumber,
                  nodeInfo.positionIndex,
                  nodeInfo.parentId,
                  nodeInfo.childIds.isEmpty()
                      ? null
                      : String.join(",", nodeInfo.childIds)
              }));
      log.debug(
          "Added row: element_id={} type={} heading_level={} text={} parent_id={} child_ids={}",
          nodeInfo.elementId,
          elementType,
          headingLevel,
          text,
          nodeInfo.parentId,
          nodeInfo.childIds);
    }

    // 자식 요소들 재귀적으로 처리
    List<Object> children = getChildren(node);
    for (Object child : children) {
      generateRows(child, rows, nodeInfoMap, pageNumber);
    }
  }

  private String getElementType(Object node) {
    if (node instanceof XWPFDocument) {
      return "Document";
    } else if (node instanceof XWPFParagraph) {
      return "Paragraph";
    } else if (node instanceof XWPFTable) {
      return "Table";
    } else if (node instanceof XWPFTableRow) {
      return "TableRow";
    } else if (node instanceof XWPFTableCell) {
      return "TableCell";
    }
    return node.getClass().getSimpleName();
  }

  private boolean isEligibleForRow(Object node) {
    return node instanceof XWPFParagraph
        || node instanceof XWPFTable
        || node instanceof XWPFTableRow
        || node instanceof XWPFTableCell;
  }

  private List<Object> getChildren(Object node) {
    List<Object> children = new ArrayList<>();

    if (node instanceof XWPFDocument) {
      XWPFDocument doc = (XWPFDocument) node;
      children.addAll(doc.getParagraphs());
      children.addAll(doc.getTables());
    } else if (node instanceof XWPFTable) {
      XWPFTable table = (XWPFTable) node;
      children.addAll(table.getRows());
    } else if (node instanceof XWPFTableRow) {
      XWPFTableRow row = (XWPFTableRow) node;
      children.addAll(row.getTableCells());
    }

    return children;
  }

  private String extractValue(Object node) {
    if (node instanceof XWPFParagraph) {
      XWPFParagraph paragraph = (XWPFParagraph) node;
      return paragraph.getText();
    } else if (node instanceof XWPFTable) {
      return tableToString((XWPFTable) node);
    } else if (node instanceof XWPFTableRow) {
      return tableRowToString((XWPFTableRow) node);
    } else if (node instanceof XWPFTableCell) {
      XWPFTableCell cell = (XWPFTableCell) node;
      return cell.getText();
    }
    return "";
  }

  private String tableToString(XWPFTable table) {
    StringBuilder sb = new StringBuilder();
    for (XWPFTableRow row : table.getRows()) {
      for (XWPFTableCell cell : row.getTableCells()) {
        sb.append(cell.getText()).append(" | ");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

  private String tableRowToString(XWPFTableRow row) {
    StringBuilder sb = new StringBuilder();
    for (XWPFTableCell cell : row.getTableCells()) {
      sb.append(cell.getText()).append(" | ");
    }
    return sb.toString();
  }

  @Override
  public void setPluginConfig(Config pluginConfig) {
    super.setPluginConfig(pluginConfig);
  }

  @Override
  public SeaTunnelRowType getSeaTunnelRowTypeInfo(String path) throws FileConnectorException {
    return new SeaTunnelRowType(
        new String[] {
            "element_id",
            "element_type",
            "heading_level",
            "text",
            "page_number",
            "position_index",
            "parent_id",
            "child_ids"
        },
        new SeaTunnelDataType[] {
            BasicType.STRING_TYPE,
            BasicType.STRING_TYPE,
            BasicType.INT_TYPE,
            BasicType.STRING_TYPE,
            BasicType.INT_TYPE,
            BasicType.INT_TYPE,
            BasicType.STRING_TYPE,
            BasicType.STRING_TYPE
        });
  }
}
