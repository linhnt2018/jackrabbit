/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.api.query;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Query;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Abstract base class for query test cases.
 */
public abstract class AbstractQueryTest extends AbstractJCRTest {

    /**
     * Namespace URI for jcr functions in XPath
     */
    protected static final String NS_JCRFN_URI = "http://www.jcp.org/jcr/xpath-functions/1.0";

    /**
     * Resolved QName for jcr:score
     */
    protected String jcrScore;

    /**
     * Resolved QName for jcr:path
     */
    protected String jcrPath;

    /**
     * Resolved QName for jcr:root
     */
    protected String jcrRoot;

    /**
     * Resolved QName for jcrfn:contains
     */
    protected String jcrfnContains;

    /**
     * Set-up the configuration values used for the test. Per default retrieves
     * a session, configures testRoot, and nodetype and checks if the query
     * language for the current language is available.<br>
     */
    protected void setUp() throws Exception {
        super.setUp();
        jcrScore = superuser.getNamespacePrefix(NS_JCR_URI) + ":score";
        jcrPath = superuser.getNamespacePrefix(NS_JCR_URI) + ":path";
        jcrRoot = superuser.getNamespacePrefix(NS_JCR_URI) + ":root";
        jcrfnContains = superuser.getNamespacePrefix(NS_JCRFN_URI) + ":contains";
    }

    /**
     * Create a {@link Query} for a given {@link Statement}.
     *
     * @param statement the query should be created for
     * @return
     *
     * @throws RepositoryException
     * @see #createQuery(String, String)
     */
    protected Query createQuery(Statement statement) throws RepositoryException {
        return createQuery(statement.getStatement(), statement.getLanguage());
    }

    /**
     * Creates a {@link Query} for the given statement in the requested
     * language
     *
     * @param statement the query should be created for
     * @param language  query language to be used for Query creation
     * @return
     *
     * @throws RepositoryException
     */
    protected Query createQuery(String statement, String language) throws RepositoryException {
        log.println("Creating query: " + statement);
        return superuser.getWorkspace().getQueryManager().createQuery(statement,
                language);
    }

    /**
     * Creates and executes a {@link Query} for the given {@link Statement}
     *
     * @param statement to execute
     * @return
     *
     * @throws RepositoryException
     * @see #execute(String, String)
     */
    protected QueryResult execute(Statement statement) throws RepositoryException {
        return execute(statement.getStatement(), statement.getLanguage());
    }

    /**
     * Creates and executes a {@link Query} for a given Statement in a given
     * query language
     *
     * @param statement the query should be build for
     * @param language  query language the stement is written in
     * @return
     *
     * @throws RepositoryException
     */
    protected QueryResult execute(String statement, String language)
            throws RepositoryException {
        Query query = createQuery(statement, language);
        return query.execute();
    }

    /**
     * Checks if the <code>result</code> contains a number of
     * <code>hits</code>.
     *
     * @param result the <code>QueryResult</code>.
     * @param hits   the number of expected hits.
     * @throws RepositoryException if an error occurs while iterating over the
     *                             result nodes.
     */
    protected void checkResult(QueryResult result, int hits)
            throws RepositoryException {
        RowIterator itr = result.getRows();
        long count = itr.getSize();
        if (count == 0) {
            log.println(" NONE");
        }
        assertEquals("Wrong hit count.", hits, count);
    }

    /**
     * Checks if the <code>result</code> contains a number of <code>hits</code>
     * and <code>properties</code>.
     *
     * @param result     the <code>QueryResult</code>.
     * @param hits       the number of expected hits.
     * @param properties the number of expected properties.
     * @throws RepositoryException if an error occurs while iterating over the
     *                             result nodes.
     */
    protected void checkResult(QueryResult result, int hits, int properties)
            throws RepositoryException {
        checkResult(result, hits);
        // now check property count
        int count = 0;
        log.println("Properties:");
        String[] propNames = result.getPropertyNames();
        for (RowIterator it = result.getRows(); it.hasNext();) {
            StringBuffer msg = new StringBuffer();
            Value[] values = it.nextRow().getValues();
            for (int i = 0; i < propNames.length; i++, count++) {
                msg.append("  ").append(propNames[i]).append(": ");
                if (values[i] == null) {
                    msg.append("null");
                } else {
                    msg.append(values[i].getString());
                }
            }
            log.println(msg);
        }
        if (count == 0) {
            log.println("  NONE");
        }
        assertEquals("Wrong property count.", properties, count);
    }

    /**
     * Checks if the {@link QueryResult} is ordered according order property in
     * direction of related argument.
     *
     * @param queryResult to be tested
     * @param propName    Name of the porperty to order by
     * @param descending  if <code>true</code> order has to be descending
     * @throws RepositoryException
     * @throws NotExecutableException in case of less than two results or all
     *                                results have same size of value in its
     *                                order-property
     */
    protected void evaluateResultOrder(QueryResult queryResult, String propName,
                                       boolean descending)
            throws RepositoryException, NotExecutableException {
        RowIterator rows = queryResult.getRows();
        if (getSize(rows) < 2) {
            throw new NotExecutableException("Can not test ordering on less than 2 results");
        }
        // need to re-aquire rows, {@link #getSize} may consume elements.
        rows = queryResult.getRows();
        int changeCnt = 0;
        String last = "";
        while (rows.hasNext()) {
            String value = rows.nextRow().getValue(propName).getString();
            int cp = value.compareTo(last);
            // if value changed evauluate if the derection is correct
            if (cp != 0) {
                changeCnt++;
                if (cp == 1 && descending) {
                    fail("Repository doesn't order properly descending");
                } else if (cp == -1 && !descending) {
                    fail("Repository doesn't order properly ascending");
                }
            }
            last = value;
        }
        if (changeCnt < 1) {
            throw new NotExecutableException("Can not test ordering on only one value");
        }
    }

    /**
     * Test if the requested Descriptor is registred at repository
     *
     * @param descriptor to be searched.
     * @return true if descriptor is contained in the repository
     */
    protected boolean hasDescriptor(String descriptor) {
        return superuser.getRepository().getDescriptor(descriptor) != null;
    }
}
