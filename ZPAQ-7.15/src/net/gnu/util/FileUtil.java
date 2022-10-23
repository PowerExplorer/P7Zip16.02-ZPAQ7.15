package net.gnu.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.Writer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.Closeable;
import java.io.ByteArrayOutputStream;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.Set;
import java.util.List;


public class FileUtil {
	public static void close(Closeable... closable) {
		if (closable != null && closable.length > 0) {
			for (Closeable c : closable) {
				try {
					if (c != null) {
						c.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void flushClose(OutputStream... closable) {
		if (closable != null && closable.length > 0) {
			for (OutputStream c : closable) {
				if (c != null) {
					try {
						c.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						c.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void flushClose(Writer... closable) {
		if (closable != null && closable.length > 0) {
			for (Writer c : closable) {
				if (c != null) {
					try {
						c.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						c.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void is2File(InputStream is, String fileName)
	throws IOException {
		File file = new File(fileName);
		file.getParentFile().mkdirs();
		File tempFile = new File(fileName + ".tmp");
		FileOutputStream fos = new FileOutputStream(tempFile);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		BufferedInputStream bis = new BufferedInputStream(is);
		byte[] barr = new byte[32768];
		int read = 0;
		try {
			while ((read = bis.read(barr)) > 0) {
				bos.write(barr, 0, read);
			}
		} finally {
			close(is, bis);
			flushClose(bos, fos);
			file.delete();
			tempFile.renameTo(file);
		}
	}

	public static byte[] is2Barr(InputStream is, boolean autoClose) throws IOException {
		int count = 0;
		int len = 65536;
		byte[] buffer = new byte[len];
		BufferedInputStream bis = new BufferedInputStream(is);
		ByteArrayOutputStream bb = new ByteArrayOutputStream(20 << 1);
		while ((count = bis.read(buffer, 0, len)) != -1) {
			bb.write(buffer, 0, count);
		}
		if (autoClose) {
			FileUtil.close(bis, is);
		} 

		return bb.toByteArray();
	}

	public static void stringToFile(String fileName, String contents)
	throws IOException {
		Log.d("writeContentToFile", fileName);
		File f = new File(fileName);
		f.getParentFile().mkdirs();
		File tempFile = new File(fileName + ".tmp");
		FileWriter fw = new FileWriter(tempFile);
		BufferedWriter bw = new BufferedWriter(fw);
		if (contents != null && contents.length() > 0) {
			bw.write(contents);
			flushClose(bw); //, bo fw vi loi
			f.delete();
			tempFile.renameTo(f);
		}
	}

	public static Collection<File> getFiles(File[] fs, boolean includeFolder) {
		if (fs == null) {
			return new LinkedList<File>();
		}
		final Set<File> set = new TreeSet<File>();
		final LinkedList<File> folderQueue = new LinkedList<File>();
		for (File f : fs) {
			if (f.isDirectory()) {
				folderQueue.push(f);
				if (includeFolder) {
					set.add(f);
				}
			} else {
				set.add(f);
			}
		}
		File fi = null;
		while (folderQueue.size() > 0) {
			fi = folderQueue.pop();
			fs = fi.listFiles();
			if (fs != null)
				for (File f : fs) {
					if (f.isDirectory()) {
						folderQueue.push(f);
						if (includeFolder) {
							set.add(f);
						}
					} else {
						set.add(f);
					}
				}
		}
		return set;
	}

	public static Collection<File> getFiles(File f, boolean includeFolder) {
		Log.d("getFiles f", f.getAbsolutePath());
		final List<File> fList = new LinkedList<File>();
		if (f != null) {
			final LinkedList<File> folderQueue = new LinkedList<File>();
			if (f.isDirectory()) {
				if (includeFolder) {
					fList.add(f);
				}
				folderQueue.push(f);
			} else {
				fList.add(f);
			}
			File fi = null;
			File[] fs;
			while (folderQueue.size() > 0) {
				fi = folderQueue.pop();
				fs = fi.listFiles();
				if (fs != null) {
					for (File f2 : fs) {
						if (f2.isDirectory()) {
							folderQueue.push(f2);
							if (includeFolder) {
								fList.add(f2);
							}
						} else {
							fList.add(f2);
						}
					}
				}
			}
		}
		return fList;
	}
	

}

