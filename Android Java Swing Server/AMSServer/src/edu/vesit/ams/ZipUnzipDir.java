package edu.vesit.ams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUnzipDir
{
	ArrayList<File> fileList = new ArrayList<File>();
	String zipFileLocation;
	
	public File createZipOfDir(String zipFileLocation, File folderToZip)
	{
		generateFileList(folderToZip);
		try
		{
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileLocation));
			zos.setLevel(9);
			for(File f:fileList)
			{
				zos.putNextEntry(new ZipEntry(f.getAbsolutePath().substring(folderToZip.getAbsolutePath().length()+1, f.getAbsolutePath().length())));	//keeping only relative path
				FileInputStream fis = new FileInputStream(f);
				byte[] buffer = new byte[1024];
				int len;
				while((len=fis.read(buffer))>0)
					zos.write(buffer, 0, len);
				fis.close();
				zos.closeEntry();
			}
			zos.close();
			return new File(zipFileLocation);
		}catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	public void generateFileList(File f)
	{
		File files[] = f.listFiles();
		for(File file:files)
			if(file.isFile())
				fileList.add(file);
			else
				generateFileList(file);
	}
	public boolean unzipToDir(File zipFile, File unzippedDestnDir)	//true on success else false
	{
		//ANDROID IS BASED ON LINUX SO CONVERTING \ TO / IN FILENAME WHILE EXTRACTING
        if(!unzippedDestnDir.exists())
        	unzippedDestnDir.mkdirs();
        
        byte[] buffer = new byte[1024];
        try
        {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze = zis.getNextEntry();
            while(ze != null)
            {
                String fileName = ze.getName().replace('\\', '/');
                File unzippedFile = new File(unzippedDestnDir, fileName);
                
                //create directories for sub directories in zip
                new File(unzippedFile.getParent()).mkdirs();
                
                FileOutputStream fos = new FileOutputStream(unzippedFile);
                int len;
                while ((len = zis.read(buffer)) > 0)
                	fos.write(buffer, 0, len);
                fos.close();
                
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            return true;
        }catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
	}
}
