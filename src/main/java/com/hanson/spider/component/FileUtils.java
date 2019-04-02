package com.hanson.spider.component;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hanson.spider.misc.SpiderResponseCode;
import com.hanson.base.exception.ServiceException;

/**
 * @author Hanson
 * create on 2019年2月6日
 */
@Component
public class FileUtils {
	@Value("${folder.path}")
	private String folderPath;
	
	public void saveFile(String folderName,String fileName,String content) {
		 //保存文件备份
       
        File folder = new File(folderPath+"//"+folderName+"//");
        folder.mkdirs();
		String filePath = folder.getPath()+"//"+fileName;
        FileOutputStream fos;
		try {
			fos = new FileOutputStream(new File(filePath));
			IOUtils.write(content,fos);
			fos.close();
		} catch (Exception exception) {
			throw new ServiceException(SpiderResponseCode.WRITE_FILE_ERROR,String.format(SpiderResponseCode.WRITE_FILE_ERROR.detailMsg(), exception.getMessage()),exception);
		}
	}
}

