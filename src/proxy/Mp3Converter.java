package proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Mp3Converter extends Thread {
	private final File tmpFile;
	private final File mp3File;
	
	public Mp3Converter(File tmpFile) {
		this.tmpFile = tmpFile;
		if(tmpFile != null && tmpFile.exists()) {
			String name = tmpFile.getName();
			String mp3Name = name.substring(0, name.lastIndexOf('.')) + ".mp3";
			mp3File = new File(mp3Name);
			if(mp3File.exists()) mp3File.delete();
			start();
		} else {
			mp3File = null;
		}
	}
	
	@Override
	public void run() {
		try {
			BufferedInputStream reader = new BufferedInputStream(new FileInputStream(tmpFile));
			BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(mp3File));
			boolean passHeader = false;
			
			int c;
			while (reader.available() > 0) {
				c = reader.read();
				if(!passHeader && c == 't') {
					c = reader.read();
					if(c == 'e') {
						c = reader.read();
						if(c == 'x') {
							c = reader.read();
							if(c == 't') {
								c = reader.read();
								if(c == '/') {
									c = reader.read();
									if(c == 'h') {
										c = reader.read();
										if(c == 't') {
											c = reader.read();
											if(c == 'm') {
												c = reader.read();
												if(c == 'l') {
													c = reader.read();
													c = reader.read();
													c = reader.read();
													passHeader = true;
													continue;
												}
											}
										}
									}
								}
							}
						}
					}
				}
				if(passHeader) {
					do {
						c = reader.read();
						writer.write(c);
					} while(c != -1);
				}
			}
			reader.close();
			writer.flush();
			writer.close();
			tmpFile.delete();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
