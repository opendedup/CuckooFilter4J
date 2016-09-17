/*
   Copyright 2016 Mark Gunlogson

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.TWARE.
*/

package com.cuckooforjava;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;

import com.cuckooforjava.CuckooFilter.Algorithm;
import com.google.common.hash.Funnels;
import com.google.common.testing.ClassSanityTester;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.SerializableTester;

public class TestIndexTagCalc {

	@Test(expected = IllegalArgumentException.class)
	public void filterTooBig() {
		new IndexTagCalc<Integer>(Algorithm.Murmur3_32, Funnels.integerFunnel(), Integer.MAX_VALUE, 1);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void filterTooBig2() {
		new IndexTagCalc<Integer>(Algorithm.Murmur3_32, Funnels.integerFunnel(), 1, Integer.MAX_VALUE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidArgs() {
		new IndexTagCalc<Integer>(Algorithm.Murmur3_32, Funnels.integerFunnel(), 0, 1);
	}
	@Test(expected = IllegalArgumentException.class)
	public void testInvalidArgs2() {
		new IndexTagCalc<Integer>(Algorithm.Murmur3_32, Funnels.integerFunnel(), 1, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTagTooBig() {
		new IndexTagCalc<Integer>(Algorithm.Murmur3_32, Funnels.integerFunnel(), 2, 28);
	}

	@Test
	public void knownZeroTags() {
		// manual instantiation to force salts to be static
		SerializableSaltedHasher<Integer> hasher = new SerializableSaltedHasher<>(0, 0, Funnels.integerFunnel(),
				Algorithm.Murmur3_32);
		IndexTagCalc<Integer> indexer = new IndexTagCalc<>(hasher, 128, 4);
		// find some 0 value tags
		int zeroTags = 0;
		int i = 0;
		ArrayList<Integer> zeroTagInputs = new ArrayList<>();
		while (zeroTags < 20) {
			if (indexer.getTagValue(hasher.hashObj(i).asInt()) == 0) {
				zeroTagInputs.add(i);
				zeroTags++;
			}
			i++;
		}
		for (Integer tag : zeroTagInputs) {
			// none of the zero value tags should be zero from indexer if
			// rehashing is working properly
			assertFalse(indexer.generate(tag).tag == 0);
		}

	}

	@Test
	public void tagIndexBitsUsed() {
		// manual instantiation to force salts to be static
		SerializableSaltedHasher<Integer> hasher = new SerializableSaltedHasher<>(0, 0, Funnels.integerFunnel(),
				Algorithm.Murmur3_32);
		IndexTagCalc<Integer> indexer = new IndexTagCalc<>(hasher, 128, 4);
		int setBitsIndex = 0;
		int setBitsTag = 0;
		// should be enough to set all bits being used...
		for (int i = 0; i < 12345; i++) {
			BucketAndTag bt = indexer.generate(i);
			setBitsIndex |= bt.index;
			setBitsTag |= bt.tag;
		}
		// will be true if we're using the right number of bits for tag and
		// index for this calculator
		assertTrue(Integer.bitCount(setBitsIndex) == 7);
		assertTrue(Integer.bitCount(setBitsTag) == 4);
		// check where the set bits are (tag should be high bits, index low
		// bits)
		int indexMask = 0b00000000000000000000000001111111;
		int tagMask = 0b00000000000000000000000000001111;
		assertTrue(indexMask == setBitsIndex);
		assertTrue(tagMask == setBitsTag);
	}

	@Test
	public void testEquals() {
		new EqualsTester().addEqualityGroup(new IndexTagCalc<Integer>(getUnsaltedHasher(), 128, 4))
				.addEqualityGroup(new IndexTagCalc<Integer>(getUnsaltedHasher(), 256, 4))
				.addEqualityGroup(new IndexTagCalc<Integer>(getUnsaltedHasher(), 128, 6)).testEquals();
	}

	@Test
	public void testEqualsSame() {
		assertTrue(new IndexTagCalc<Integer>(getUnsaltedHasher(), 128, 4)
				.equals(new IndexTagCalc<Integer>(getUnsaltedHasher(), 128, 4)));
	}

	@Test
	public void testCopy() {
		IndexTagCalc<Integer> calc = new IndexTagCalc<>(Algorithm.Murmur3_32, Funnels.integerFunnel(), 128, 4);
		IndexTagCalc<Integer> calcCopy = calc.copy();
		assertTrue(calcCopy.equals(calc));
		assertNotSame(calc, calcCopy);
	}

	@Test
	public void autoTestNulls() {
		// chose 8 for int so it passes bucket and tag size checks
		new ClassSanityTester().setDefault(SerializableSaltedHasher.class, getUnsaltedHasher()).setDefault(int.class, 8)
				.testNulls(IndexTagCalc.class);
	}

	@Test
	public void brokenAltIndex() {
		Random rando = new Random();
		IndexTagCalc<Integer> calc = new IndexTagCalc<>(Algorithm.Murmur3_32, Funnels.integerFunnel(), 2048, 14);
		for (int i = 0; i < 10000; i++) {
			BucketAndTag pos = calc.generate(rando.nextInt());
			int altIndex = calc.altIndex(pos.index, pos.tag);
			assertTrue(pos.index == calc.altIndex(altIndex, pos.tag));
		}
	}

	@Test
	public void testSerialize() {
		SerializableTester.reserializeAndAssert(new IndexTagCalc<Integer>(getUnsaltedHasher(), 128, 4));
	}

	// using this because otherwise internal state of salts in hasher will
	// prevent equality comparisons
	private SerializableSaltedHasher<Integer> getUnsaltedHasher() {
		return new SerializableSaltedHasher<>(0, 0, Funnels.integerFunnel(), Algorithm.Murmur3_32);
	}

}