package net.sourceforge.tess4j;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hanson
 * create on 2019年3月4日
 */

public class ITesseractTest {
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Test
	@Ignore
	public void test() throws TesseractException {
		ITesseract instance = new Tesseract();
	    instance.setLanguage("chi_sim");
	    String datapath = this.getClass().getResource("/").getPath();
	    datapath = datapath.substring(1, datapath.length());
		instance.setDatapath(datapath);
		// 识别验证码
		String ocrResult = instance.doOCR(new File("D:\\body\\syfc\\verifyCode\\19025.jpg"));
		logger.info(ocrResult);
	}

}

