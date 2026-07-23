package com.querycraft.parser;

import com.querycraft.exception.SchemaParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class LiquibaseXmlSchemaParser implements SchemaParser {

    private static final Logger log = LoggerFactory.getLogger(LiquibaseXmlSchemaParser.class);

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".xml")) {
            return true;
        }
        return contentType != null && (contentType.equalsIgnoreCase("text/xml") ||
                                       contentType.equalsIgnoreCase("application/xml"));
    }

    @Override
    public String parse(InputStream inputStream, String fileName) throws SchemaParsingException {
        log.info("Parsing Liquibase XML changelog file: {}", fileName);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Disable DTD / External Entities for XML security (XXE protection)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);
            doc.getDocumentElement().normalize();

            StringBuilder ddlOutput = new StringBuilder();
            ddlOutput.append("--- Liquibase XML Schema Definition Source: ").append(fileName).append(" ---\n");

            // Extract <createTable> tags
            NodeList createTables = doc.getElementsByTagName("createTable");
            for (int i = 0; i < createTables.getLength(); i++) {
                Element tableElement = (Element) createTables.item(i);
                String tableName = tableElement.getAttribute("tableName");
                String remarks = tableElement.getAttribute("remarks");

                ddlOutput.append("CREATE TABLE ").append(tableName).append(" (\n");

                NodeList columns = tableElement.getElementsByTagName("column");
                List<String> colDefs = new ArrayList<>();
                for (int j = 0; j < columns.getLength(); j++) {
                    Element colElement = (Element) columns.item(j);
                    String colName = colElement.getAttribute("name");
                    String colType = colElement.getAttribute("type");
                    String colRemarks = colElement.getAttribute("remarks");

                    StringBuilder colSb = new StringBuilder();
                    colSb.append("  ").append(colName).append(" ").append(colType);

                    // Check for constraints child element
                    NodeList constraintsList = colElement.getElementsByTagName("constraints");
                    if (constraintsList.getLength() > 0) {
                        Element constraints = (Element) constraintsList.item(0);
                        if ("true".equalsIgnoreCase(constraints.getAttribute("primaryKey"))) {
                            colSb.append(" PRIMARY KEY");
                        }
                        if ("false".equalsIgnoreCase(constraints.getAttribute("nullable"))) {
                            colSb.append(" NOT NULL");
                        }
                        if ("true".equalsIgnoreCase(constraints.getAttribute("unique"))) {
                            colSb.append(" UNIQUE");
                        }
                    }

                    if (!colRemarks.isBlank()) {
                        colSb.append(" -- ").append(colRemarks);
                    }
                    colDefs.add(colSb.toString());
                }
                ddlOutput.append(String.join(",\n", colDefs)).append("\n);");
                if (!remarks.isBlank()) {
                    ddlOutput.append(" -- Table Remarks: ").append(remarks);
                }
                ddlOutput.append("\n\n");
            }

            // Extract <addColumn> tags
            NodeList addColumns = doc.getElementsByTagName("addColumn");
            for (int i = 0; i < addColumns.getLength(); i++) {
                Element addColElement = (Element) addColumns.item(i);
                String tableName = addColElement.getAttribute("tableName");

                NodeList columns = addColElement.getElementsByTagName("column");
                for (int j = 0; j < columns.getLength(); j++) {
                    Element colElement = (Element) columns.item(j);
                    String colName = colElement.getAttribute("name");
                    String colType = colElement.getAttribute("type");
                    ddlOutput.append("ALTER TABLE ").append(tableName)
                             .append(" ADD COLUMN ").append(colName).append(" ").append(colType).append(";\n");
                }
            }

            // Extract <addForeignKeyConstraint> tags
            NodeList fkList = doc.getElementsByTagName("addForeignKeyConstraint");
            for (int i = 0; i < fkList.getLength(); i++) {
                Element fk = (Element) fkList.item(i);
                String baseTable = fk.getAttribute("baseTableName");
                String baseColumn = fk.getAttribute("baseColumnNames");
                String refTable = fk.getAttribute("referencedTableName");
                String refColumn = fk.getAttribute("referencedColumnNames");

                ddlOutput.append("ALTER TABLE ").append(baseTable)
                         .append(" ADD FOREIGN KEY (").append(baseColumn)
                         .append(") REFERENCES ").append(refTable).append("(").append(refColumn).append(");\n");
            }

            String result = ddlOutput.toString().trim();
            if (result.length() <= 50) {
                // Fallback: If no recognized Liquibase tags found, render plain XML content as schema text
                log.warn("No Liquibase createTable/addColumn tags found in XML. Storing raw XML structure.");
                return "--- Liquibase XML File Content: " + fileName + " ---\n" + result;
            }
            return result;

        } catch (Exception e) {
            log.error("Failed to parse Liquibase XML file {}: {}", fileName, e.getMessage());
            throw new SchemaParsingException("Failed to parse Liquibase XML schema from file: " + fileName, e);
        }
    }

    @Override
    public String getFileType() {
        return "LIQUIBASE_XML";
    }
}
