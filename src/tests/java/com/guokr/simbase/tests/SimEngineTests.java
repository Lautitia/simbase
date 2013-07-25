package com.guokr.simbase.tests;

import static org.junit.Assert.*;
import org.junit.Test;

import com.guokr.simbase.SimTable;

public class SimEngineTests {

	@Test
	public void test() {
		SimTable engine = new SimTable();
		engine.add(2, new float[] { 0.9f, 0.1f, 0f });
		engine.add(3, new float[] { 0.9f, 0f, 0.1f });
		engine.add(5, new float[] { 0.1f, 0.9f, 0f });
		engine.add(7, new float[] { 0.1f, 0f, 0.9f });
		engine.add(11, new float[] { 0f, 0.9f, 0.1f });
		engine.add(13, new float[] { 0f, 0.1f, 0.9f });

		System.out.println(engine.retrieve(2));
		System.out.println(engine.retrieve(3));
		System.out.println(engine.retrieve(5));
		System.out.println(engine.retrieve(7));
		System.out.println(engine.retrieve(11));
		System.out.println(engine.retrieve(13));

		assertEquals(engine.retrieve(2).get(2), new Float(1.0));
		assertEquals(engine.retrieve(3).get(3), new Float(1.0));
		assertEquals(engine.retrieve(5).get(5), new Float(1.0));
		assertEquals(engine.retrieve(7).get(7), new Float(1.0));
		assertEquals(engine.retrieve(11).get(11), new Float(1.0));
		assertEquals(engine.retrieve(13).get(13), new Float(1.0));

		assertTrue((int)(1000 * engine.similarity(2, 3)) == (int)(1000 * engine.similarity(5, 11)));
		assertTrue((int)(1000 * engine.similarity(2, 3)) == (int)(1000 * engine.similarity(7, 13)));
		assertTrue((int)(1000 * engine.similarity(2, 5)) == (int)(1000 * engine.similarity(3, 7)));
		assertTrue((int)(1000 * engine.similarity(2, 5)) == (int)(1000 * engine.similarity(11, 13)));
		assertTrue((int)(1000 * engine.similarity(2, 7)) == (int)(1000 * engine.similarity(3, 5)));
		assertTrue((int)(1000 * engine.similarity(2, 7)) == (int)(1000 * engine.similarity(7, 11)));
		assertTrue((int)(1000 * engine.similarity(2, 11)) == (int)(1000 * engine.similarity(3, 13)));
		assertTrue((int)(1000 * engine.similarity(2, 13)) == (int)(1000 * engine.similarity(5, 7)));

	}

}