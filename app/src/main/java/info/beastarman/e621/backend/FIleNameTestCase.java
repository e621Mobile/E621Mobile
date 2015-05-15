package info.beastarman.e621.backend;

import android.test.InstrumentationTestCase;

public class FIleNameTestCase extends InstrumentationTestCase
{
	public void runTest(String s)
	{
		assertEquals(s, FileName.decodeFileName(FileName.encodeFileName(s)));
	}

	public void test() throws Exception
	{
		runTest("123");
		runTest("a b c d e");
		runTest("$$?%?$$");
		runTest("five`AAA");
		runTest("five_nights_at_freddy's");
	}
}
