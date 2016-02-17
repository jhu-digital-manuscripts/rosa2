/*
 * Copyright 2014 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rosa.search.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import rosa.search.model.Query;
import rosa.search.model.QueryOperation;

public class QueryParserTest {
    QueryParser parser;

    @Test
    public void testParseSingleTerm() throws ParseException {
        Query result = QueryParser.parseQuery("field:'value'");
        Query expected = new Query("field", "value");

        assertEquals(expected, result);
    }

    @Test
    public void testParseSingleTermSkippingWhitespace() throws ParseException {
        Query result = QueryParser.parseQuery("     field:'v  alue' ");
        Query expected = new Query("field", "v  alue");

        assertEquals(expected, result);
    }

    @Test
    public void testParseNestedQuery1() throws ParseException {
        Query result = QueryParser.parseQuery("(title:'cow' & description:'good bovine')");
        Query expected = new Query(QueryOperation.AND, new Query("title", "cow"),
                new Query("description", "good bovine"));

        assertEquals(expected, result);
    }

    @Test
    public void testParseNestedQuery2() throws ParseException {
        Query result = QueryParser.parseQuery("((title:'cow' | title:'farm') & description:'good bovine')");
        Query expected = new Query(QueryOperation.AND,
                new Query(QueryOperation.OR, new Query("title", "cow"), new Query("title", "farm")),
                new Query("description", "good bovine"));

        assertEquals(expected, result);
    }

    @Test
    public void testEscapingTermValue() throws ParseException {
        Query result = QueryParser.parseQuery("field:'\\\\ \\\''");
        Query expected = new Query("field", "\\ \'");

        assertEquals(expected, result);
    }

    @Test(expected = ParseException.class)
    public void testParseInvalidEmptyQuery() throws ParseException {
        QueryParser.parseQuery("  ");
    }

    @Test(expected = ParseException.class)
    public void testParseInvalidTerm() throws ParseException {
        QueryParser.parseQuery("field:value");
    }

    @Test(expected = ParseException.class)
    public void testParseInvalidNested() throws ParseException {
        QueryParser.parseQuery("( f:'v' & g:'g'");
    }
    
    @Test(expected = ParseException.class)
    public void testParseInvalidRandomString() throws ParseException {
        QueryParser.parseQuery("cow: mammal!");
    }
    
    @Test(expected = ParseException.class)
    public void testInputAfterTerm() throws ParseException {
        QueryParser.parseQuery("cow:'mammal' title:'moo'");
    }
    
    @Test(expected = ParseException.class)
    public void testInputAfterOp() throws ParseException {
        QueryParser.parseQuery("(cow:'mammal' | title:'moo') blah:'blah'");
    }
}
