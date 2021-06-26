/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.java.html.js.tests;

import java.util.List;
import java.util.function.Function;
import net.java.html.js.JavaScriptBody;
import static net.java.html.js.tests.JavaScriptBodyTest.assertEquals;

final class AsyncJavaScriptAction {
    private List<Integer> collected = new java.util.ArrayList<>();
    private boolean successStoringLater;
    private AssertionError invertedVia;

    @JavaScriptBody(args = { "n" }, javacall = true, body = """
        return this.@net.java.html.js.tests.AsyncJavaScriptAction::performIteration(I)(n);
    """)
    private native int enterJavaScriptAndPerformIteration(int n);

    @JavaScriptBody(args = {}, javacall = true, body = """
        var self = this;
        var global = (0 || eval)("this");
        global.storeLater = function(s) {
            self.@net.java.html.js.tests.AsyncJavaScriptAction::storeLater(I)(s);
        };
    """)
    private native void defineStore();

    @JavaScriptBody(args = { "store" }, javacall = true, wait4js = false, body = """
        storeLater(store);
    """)
    private static native void jsStore(int store);

    void storeLater(int value) {
        if (!collected.isEmpty()) {
            if (collected.get(collected.size() - 1) > value) {
                invertedVia = new AssertionError("Not in order " + value, invertedVia);
            }
        }
        collected.add(value);
    }

    @JavaScriptBody(args = {}, body = "")
    native void flushPendingJavaScripts();

    int performIteration(int middle) {
        for (int i = -5; i < 0; i++) {
            jsStore(middle + i);
        }
        String n = "" + middle;
        successStoringLater = JsUtils.executeNow(AsyncJavaScriptAction.class, "storeLater(" + n + ");");
        for (int i = 1; i <= 5; i++) {
            jsStore(middle + i);
        }
        return middle + 5;
    }

    private void performTheTest(Function<Integer,Integer> iteration) {
        defineStore();
        assertEquals(iteration.apply(0), 5);
        if (!successStoringLater) {
            return;
        }
        flushPendingJavaScripts();
        assertEquals(collected.size(), 11, "11 items: " + collected);
        for (int i = 0; i < 11; i++) {
            final Integer atI = collected.get(i);
            final int iMinus5 = i - 5;
            assertEquals(atI, iMinus5, i + "th out of order: " + collected);
        }
        assertEquals(iteration.apply(11), 16);
        if (!successStoringLater) {
            return;
        }
        flushPendingJavaScripts();
        assertEquals(collected.size(), 22, "22 items: " + collected);
        for (int i = 0; i < 22; i++) {
            final Integer atI = collected.get(i);
            final int iMinus5 = i - 5;
            assertEquals(atI, iMinus5, i + "th out of order: " + collected);
        }
    }

    public void testWithCallback() {
        performTheTest(new Function<Integer,Integer>() {
            @Override
            public Integer apply(Integer t) {
                return enterJavaScriptAndPerformIteration(t);
            }
        });
    }

    public void testWithoutCallback() {
        performTheTest(new Function<Integer,Integer>() {
            @Override
            public Integer apply(Integer t) {
                return performIteration(t);
            }
        });
    }

}
