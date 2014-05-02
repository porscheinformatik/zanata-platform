/*
 * Copyright 2014, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package org.zanata.feature;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;
import org.zanata.util.RetryRule;

import java.lang.annotation.Inherited;

/**
 * Global application of rules to Zanata functional tests
 *
 * @author Damian Jansen
 * <a href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
@Slf4j
public class ZanataTestCase {

    /* Maximum five minute test duration */
    private static int MAX_TEST_DURATION = 300000;

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    @Rule
    public TestName testName = new TestName();

    @Rule
    public Timeout timeout = new Timeout(MAX_TEST_DURATION);

    public DateTime testFunctionStart;

    @Before
    public void testEntry() {
        log.info("Starting " + testName.getMethodName());
        testFunctionStart = new DateTime();
    }

    @After
    public void testExit() {
        Duration duration = new Duration(testFunctionStart, new DateTime());
        PeriodFormatter periodFormatter = new PeriodFormatterBuilder()
                .appendLiteral("Finished " + testName.getMethodName() + " in " )
                .printZeroAlways()
                .appendMinutes()
                .appendSuffix(" minutes, ")
                .appendSeconds()
                .appendSuffix(" seconds, ")
                .appendMillis()
                .appendSuffix("ms")
                .toFormatter();
        log.info(periodFormatter.print(duration.toPeriod()));
    }
}