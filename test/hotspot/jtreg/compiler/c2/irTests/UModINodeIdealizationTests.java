/*
 * Copyright (c) 2022, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package compiler.c2.irTests;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;

import java.util.Random;

import compiler.lib.ir_framework.*;

/*
 * @test
 * @bug 8332268
 * @summary Test that Ideal transformations of UModINode* are being performed as expected.
 * @library /test/lib /
 * @run driver compiler.c2.irTests.UModINodeIdealizationTests
 */
public class UModINodeIdealizationTests {
    public static final int RANDOM_POWER_OF_2 = 1 << (1 + Utils.getRandomInstance().nextInt(30));

    public static void main(String[] args) {
        TestFramework.run();
    }

    @Run(test = {"constant", "constantAgain", "powerOf2", "powerOf2Big", "powerOf2Random", "reallyConstant"})
    public void runMethod() {
        int a = RunInfo.getRandom().nextInt();
        a = (a == 0) ? 2 : a;
        int b = RunInfo.getRandom().nextInt();
        b = (b == 0) ? 2 : b;

        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;

        assertResult(0, 0, true);
        assertResult(a, b, false);
        assertResult(min, min, false);
        assertResult(max, max, false);
    }

    @DontCompile
    public void assertResult(int a, int b, boolean shouldThrow) {
        try {
            Asserts.assertEQ(Integer.remainderUnsigned(a, a), constant(a));
            Asserts.assertFalse(shouldThrow, "Expected an exception to be thrown.");
        } catch (ArithmeticException e) {
            Asserts.assertTrue(shouldThrow, "Did not expect an exception to be thrown.");
        }

        Asserts.assertEQ(Integer.remainderUnsigned(a, 1), constantAgain(a));
        Asserts.assertEQ(Integer.remainderUnsigned(a, 32), powerOf2(a));
        Asserts.assertEQ(Integer.remainderUnsigned(a, Integer.MIN_VALUE), powerOf2Big(a));
        Asserts.assertEQ(Integer.remainderUnsigned(a, RANDOM_POWER_OF_2), powerOf2Random(a));
        Asserts.assertEQ(Integer.remainderUnsigned(Integer.MIN_VALUE, 302032), reallyConstant());
    }

    @Test
    @IR(failOn = {IRNode.UMOD_I, IRNode.MUL})
    @IR(counts = {IRNode.DIV_BY_ZERO_TRAP, "1"})
    // Checks x % x => 0
    public int constant(int x) {
        return Integer.remainderUnsigned(x, x);
    }

    @Test
    @IR(failOn = {IRNode.UMOD_I})
    // Checks x % 1 => 0
    public int constantAgain(int x) {
        return Integer.remainderUnsigned(x, 1);
    }

    @Test
    @IR(failOn = {IRNode.UMOD_I, IRNode.MUL})
    // Checks that modulo with two constants is calculated at compile time
    public int reallyConstant() {
        return Integer.remainderUnsigned(Integer.MIN_VALUE, 302032); // -2147483648 = Integer.parseUnsignedInt("2147483648")
    }

    @Test
    @IR(failOn = {IRNode.UMOD_I, IRNode.MUL})
    @IR(counts = {IRNode.AND, "1"})
    // Checks that for x % 2^k, 2^k-1 is used as a bit mask.
    public int powerOf2(int x) {
        return Integer.remainderUnsigned(x, 32);
    }

    @Test
    @IR(failOn = {IRNode.UMOD_I, IRNode.MUL})
    @IR(counts = {IRNode.AND, "1"})
    // Checks that for x % 2^k, 2^k-1 is used as a bit mask.
    public int powerOf2Random(int x) {
        return Integer.remainderUnsigned(x, RANDOM_POWER_OF_2);
    }

    @Test
    @IR(failOn = {IRNode.UMOD_I, IRNode.MUL})
    @IR(counts = {IRNode.AND, "1"})
    // Checks that for x % 2^k, 2^k-1 is used as a bit mask.
    public int powerOf2Big(int x) {
        return Integer.remainderUnsigned(x, Integer.MIN_VALUE);
    }
}
