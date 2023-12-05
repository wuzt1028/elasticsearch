/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.mapper;

import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xcontent.json.JsonXContent;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentParserUtils.ensureExpectedToken;

public class DynamicFieldsBuilderTests extends ESTestCase {

    public void testCreateDynamicField() throws IOException {
        assertCreateFieldType("f1", "foobar", "text");
        assertCreateFieldType("f1", "true", "text");
        assertCreateFieldType("f1", true, "boolean");
        assertCreateFieldType("f1", 123456, "long");
        assertCreateFieldType("f1", 123.456, "float");
        // numeric detection for strings is off by default
        assertCreateFieldType("f1", "123456", "text");
        assertCreateFieldType("f1", "2023-02-25", "date");
        // illegal dates should result in text field mapping
        assertCreateFieldType("f1", "2023-51", "text");
    }

    public void assertCreateFieldType(String fieldname, Object value, String expectedType) throws IOException {
        if (value instanceof String) {
            value = "\"" + value + "\"";
        }
        String source = "{\"" + fieldname + "\": " + value + "}";
        XContentParser parser = createParser(JsonXContent.jsonXContent, source);
        SourceToParse sourceToParse = new SourceToParse("test", new BytesArray(source), XContentType.JSON);
        DocumentParserContext ctx = new TestDocumentParserContext(MappingLookup.EMPTY, sourceToParse) {
            @Override
            public XContentParser parser() {
                return parser;
            }
        };

        // position the parser on the value
        ensureExpectedToken(XContentParser.Token.START_OBJECT, parser.nextToken(), parser);
        ensureExpectedToken(XContentParser.Token.FIELD_NAME, parser.nextToken(), parser);
        parser.nextToken();
        assertTrue(parser.currentToken().isValue());
        DynamicFieldsBuilder.DYNAMIC_TRUE.createDynamicFieldFromValue(ctx, fieldname);
        Map<String, List<Mapper.Builder>> dynamicMappers = ctx.getDynamicMappers();
        assertEquals(1, dynamicMappers.size());
        Mapper mapper = dynamicMappers.get(fieldname).get(0).build(MapperBuilderContext.root(false, false));
        assertEquals(fieldname, mapper.name());
        assertEquals(expectedType, mapper.typeName());
    }
}