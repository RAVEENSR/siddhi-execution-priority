/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.extension.siddhi.execution.priority;

import io.siddhi.core.SiddhiAppRuntime;
import io.siddhi.core.SiddhiManager;
import io.siddhi.core.event.Event;
import io.siddhi.core.exception.SiddhiAppCreationException;
import io.siddhi.core.query.output.callback.QueryCallback;
import io.siddhi.core.stream.input.InputHandler;
import io.siddhi.core.util.EventPrinter;
import io.siddhi.core.util.SiddhiTestHelper;
import io.siddhi.query.api.exception.SiddhiAppValidationException;
import org.apache.log4j.Logger;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class PriorityStreamProcessorTestCase {
    private static final Logger log = Logger.getLogger(PriorityStreamProcessorTestCase.class);
    private AtomicInteger eventCount;
    private boolean eventArrived;
    private int waitTime = 50;
    private int timeout = 5000;

    @BeforeMethod
    public void init() {
        eventCount = new AtomicInteger(0);
        eventArrived = false;
    }

    @Test
    public void priorityTest1() throws InterruptedException {
        log.info("Priority Window test 1: Testing increment and decrement of priority");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" +
                "define stream cseEventStream (symbol string, priority long, volume int);";
        String query = "" +
                "@info(name = 'query1') " +
                "from cseEventStream#priority:time(symbol, priority, 1 sec) " +
                "select * " +
                "insert all events into outputStream ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                if (inEvents != null) {
                    int count = eventCount.addAndGet(inEvents.length);
                    long priority = (Long) inEvents[0].getData(4);
                    if (count == 1) {
                        AssertJUnit.assertEquals("Initial priority does not match with input", 1L, priority);
                    } else if (count == 2) {
                        AssertJUnit.assertEquals("Priority is not increased by the second event", 4L, priority);
                    } else {
                        AssertJUnit.assertEquals("Priority is not increased by time", 6L - count, priority);
                    }
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("cseEventStream");
        siddhiAppRuntime.start();
        inputHandler.send(new Object[]{"IBM", 1L, 0});
        inputHandler.send(new Object[]{"IBM", 3L, 1});

        SiddhiTestHelper.waitForEvents(waitTime, 6, eventCount, timeout);
        AssertJUnit.assertEquals(6, eventCount.get());
        AssertJUnit.assertTrue(eventArrived);
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void priorityTest2() throws InterruptedException {
        log.info("Priority Window test 2: Sending first event with 0 priority");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" +
                "define stream cseEventStream (symbol string, priority long, volume int);";
        String query = "" +
                "@info(name = 'query1') " +
                "from cseEventStream#priority:time(symbol, priority, 1 sec) " +
                "select * " +
                "insert all events into outputStream ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                if (inEvents != null) {
                    eventCount.addAndGet(inEvents.length);
                    long priority = (Long) inEvents[0].getData(4);
                    AssertJUnit.assertEquals("Initial priority does not match with input", 0L, priority);
                }
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("cseEventStream");
        siddhiAppRuntime.start();
        inputHandler.send(new Object[]{"IBM", 0L, 0});

        SiddhiTestHelper.waitForEvents(waitTime, 1, eventCount, timeout);
        AssertJUnit.assertEquals(1, eventCount.get());
        AssertJUnit.assertTrue(eventArrived);
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void priorityTest3() throws InterruptedException {
        log.info("Priority Window test 3: Sending event with null key");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" +
                "define stream cseEventStream (symbol string, priority long, volume int);";
        String query = "" +
                "@info(name = 'query1') " +
                "from cseEventStream#priority:time(symbol, priority, 1 sec) " +
                "select * " +
                "insert all events into outputStream ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("cseEventStream");
        siddhiAppRuntime.start();
        inputHandler.send(new Object[]{null, 10L, 0});
        Thread.sleep(1000);
        AssertJUnit.assertFalse(eventArrived);
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void priorityTest4() throws InterruptedException {
        log.info("Priority Window test 4: Sending event with null priority");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" +
                "define stream cseEventStream (symbol string, priority long, volume int);";
        String query = "" +
                "@info(name = 'query1') " +
                "from cseEventStream#priority:time(symbol, priority, 1 sec) " +
                "select * " +
                "insert all events into outputStream ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                eventArrived = true;
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("cseEventStream");
        siddhiAppRuntime.start();
        inputHandler.send(new Object[]{"IBM", null, 0});
        Thread.sleep(1500);
        AssertJUnit.assertFalse(eventArrived);
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void priorityTest5() throws InterruptedException {
        log.info("Priority Window test 5: Testing increment and decrement of multiple events");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" +
                "define stream cseEventStream (symbol string, priority long, volume int);";
        String query = "" +
                "@info(name = 'query1') " +
                "from cseEventStream#priority:time(symbol, priority, 1 sec) " +
                "select * " +
                "insert all events into outputStream ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                if (inEvents != null) {
                    eventCount.addAndGet(inEvents.length);
                }
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("cseEventStream");
        siddhiAppRuntime.start();
        inputHandler.send(new Object[]{"IBM", 2L, 0});
        inputHandler.send(new Object[]{"WSO2", 1L, 0});
        Thread.sleep(1000);
        inputHandler.send(new Object[]{"IBM", 1L, 1});

        SiddhiTestHelper.waitForEvents(waitTime, 7, eventCount, timeout);
        AssertJUnit.assertEquals(7, eventCount.get());
        siddhiAppRuntime.shutdown();
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void priorityTest6() throws InterruptedException {
        log.info("Priority Window test 6: Testing invalid number of arguments");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" +
                "define stream cseEventStream (symbol string, priority long, volume int);";
        String query = "" +
                "@info(name = 'query1') " +
                "from cseEventStream#priority:time(symbol, priority, 1 sec, volume) " +
                "select * " +
                "insert all events into outputStream ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);
    }

    @Test(expectedExceptions = SiddhiAppValidationException.class)
    public void priorityTest7() throws InterruptedException {
        log.info("Priority Window test 7: Testing invalid first parameter");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" +
                "define stream cseEventStream (symbol string, priority long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from cseEventStream#priority:time(2 sec, priority, 1 sec, volume) " +
                "select * " +
                "insert all events into outputStream ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);
    }

    @Test(expectedExceptions = SiddhiAppCreationException.class)
    public void priorityTest8() throws InterruptedException {
        log.info("Priority Window test 8: Testing invalid second parameter");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" +
                "define stream cseEventStream (symbol string, priority long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from cseEventStream#priority:time(symbol, 1 milliseconds, 1 sec) " +
                "select * " +
                "insert all events into outputStream ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);
    }

    @Test(expectedExceptions = SiddhiAppValidationException.class)
    public void priorityTest9() throws InterruptedException {
        log.info("Priority Window test 9: Testing invalid third parameter");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" +
                "define stream cseEventStream (symbol string, priority long);";
        String query = "" +
                "@info(name = 'query1') " +
                "from cseEventStream#priority:time(symbol, priority, volume) " +
                "select * " +
                "insert all events into outputStream ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);
    }

    @Test
    public void priorityTest10() throws InterruptedException {
        log.info("Priority Window test 10: Testing increment and decrement of multiple events with integer priority");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" +
                "define stream cseEventStream (symbol string, priority int, volume int);";
        String query = "" +
                "@info(name = 'query1') " +
                "from cseEventStream#priority:time(symbol, priority, 500 milliseconds) " +
                "select * " +
                "insert all events into outputStream ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                if (inEvents != null) {
                    eventCount.addAndGet(inEvents.length);
                }
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("cseEventStream");
        siddhiAppRuntime.start();
        inputHandler.send(new Object[]{"IBM", 2, 0});
        inputHandler.send(new Object[]{"WSO2", 1, 0});
        Thread.sleep(500);
        inputHandler.send(new Object[]{"IBM", 1, 1});

        SiddhiTestHelper.waitForEvents(waitTime, 7, eventCount, timeout);
        AssertJUnit.assertEquals(7, eventCount.get());
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void priorityTest11() throws InterruptedException {
        log.info("Priority Window test 11: Testing decreasing priority to zero");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" +
                "define stream cseEventStream (symbol string, priority int, volume int);";
        String query = "" +
                "@info(name = 'query1') " +
                "from cseEventStream#priority:time(symbol, priority, 500 milliseconds) " +
                "select * " +
                "insert all events into outputStream ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                if (inEvents != null) {
                    eventCount.addAndGet(inEvents.length);
                }
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("cseEventStream");
        siddhiAppRuntime.start();
        inputHandler.send(new Object[]{"IBM", 10, 0});
        inputHandler.send(new Object[]{"IBM", -10, 0});

        SiddhiTestHelper.waitForEvents(waitTime, 2, eventCount, timeout);
        AssertJUnit.assertEquals(2, eventCount.get());
        siddhiAppRuntime.shutdown();
    }

    @Test
    public void priorityTest12() throws InterruptedException {
        log.info("Priority Window test 12: Testing decreasing priority to negative");
        SiddhiManager siddhiManager = new SiddhiManager();

        String cseEventStream = "" +
                "define stream cseEventStream (symbol string, priority int, volume int);";
        String query = "" +
                "@info(name = 'query1') " +
                "from cseEventStream#priority:time(symbol, priority, 500 milliseconds) " +
                "select * " +
                "insert all events into outputStream ;";

        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(cseEventStream + query);

        siddhiAppRuntime.addCallback("query1", new QueryCallback() {
            @Override
            public void receive(long timeStamp, Event[] inEvents, Event[] removeEvents) {
                EventPrinter.print(timeStamp, inEvents, removeEvents);
                if (inEvents != null) {
                    int total = eventCount.addAndGet(inEvents.length);
                    if (total == 2) {
                        AssertJUnit.assertEquals(0, inEvents[0].getData()[4]);   // Priority cannot be negative
                    }
                }
            }

        });

        InputHandler inputHandler = siddhiAppRuntime.getInputHandler("cseEventStream");
        siddhiAppRuntime.start();
        inputHandler.send(new Object[]{"IBM", 10, 0});
        inputHandler.send(new Object[]{"IBM", -100, 0});

        SiddhiTestHelper.waitForEvents(waitTime, 2, eventCount, timeout);
        AssertJUnit.assertEquals(2, eventCount.get());
        siddhiAppRuntime.shutdown();
    }
}
