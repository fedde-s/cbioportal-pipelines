package org.mskcc.cbio.maf;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TestMafSanitizer extends TestCase
{
	public void testSanitizer()
	{
		// TODO replace with getResourceStream()
		String input = "target/test-classes/maf_to_sanitize.txt";
		String output = "target/test-classes/maf_sanitized.txt";
		String misc = "target/test-classes/maf_misc.txt";

		try
		{
			// run sanitizer
			MafSanitizer sanitizer = new MafSanitizer();
			sanitizer.sanitizeMaf(input, output, misc);

			// check if everything is ok with the output

			BufferedReader reader = new BufferedReader(new FileReader(output));
			MafHeaderUtil headerUtil = new MafHeaderUtil();

			String line = headerUtil.extractHeader(reader);
			MafUtil util = new MafUtil(line);

			// assert the number of columns (32 standard + 1 custom)
			assertEquals(33, util.getHeaderCount());

			while ((line = reader.readLine()) != null)
			{
				if (line.length() > 0)
				{
					MafRecord record = util.parseRecord(line);
					this.validateRecord(record);
				}
			}

			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void validateRecord(MafRecord record)
	{
		MafSanitizer sanitizer = new MafSanitizer();
		MafErrorReport report = sanitizer.identifyErrors(record);

		// lines with critical error should not be included in the output
		assertTrue(!report.hasCriticalError());
	}

}
