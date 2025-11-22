package com.snapway.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

// 로컬 파일 관련 유틸 클래스
public final class FileUtil {

	private FileUtil() {
	}
	

	/**
	 * 주어진 경로의 로컬 파일을 File 객체로 반환합니다. 파일이 없거나 디렉터리면 IOException을 던집니다.
	 * @param path: 불러올 파일이 위치한 로컬 스토리지의 경로
	 */
	public static File getFile(String path) throws IOException {
		File file = new File(path);

		if (!file.exists()) {
			throw new IOException("파일이 존재하지 않습니다: " + path);
		}

		if (!file.isFile()) {
			throw new IOException("파일이 아니라 디렉터리입니다: " + path);
		}

		return file;
	}
	

	/**
	 * 바이트 배열을 받아 로컬 경로에 파일로 저장합니다. (예: 업로드된 파일 내용을 이미 byte[]로 받은 경우)
	 *
	 * @param data:     저장할 바이트 데이터
	 * @param savePath: 저장할 전체 경로. 반드시 파일이름까지 포함할 것!! (예: "C:/upload/image.png")
	 * @return 저장된 File 객체, 실패 시 null 반환
	 */
	public static File saveBytesToFile(byte[] data, String savePath) {
		File file = new File(savePath);
		// 상위 디렉터리 없으면 생성
		File parent = file.getParentFile();
		if (parent != null && !parent.exists()) {
			if (!parent.mkdirs()) {
				System.out.println("디렉터리 생성 실패: " + parent.getAbsolutePath());
				return null;
			}
		}

		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(data);
			fos.flush();
			return file;
		} catch (IOException e) {
			System.out.println("파일 저장 중 오류 발생: " + savePath);
			e.printStackTrace();
			return null;
		}
	}
	

	/**
	 * 기존 로컬 파일을 다른 경로로 복사합니다.
	 *
	 * @param sourcePath: 원본 파일 경로
	 * @param targetPath: 복사 대상 파일 경로
	 * @return 복사된 File 객체, 실패 시 null 반환
	 */
	public static File copyFile(String sourcePath, String targetPath) {
		File source = new File(sourcePath);
		if (!source.exists() || !source.isFile()) {
			System.out.println("원본 파일이 존재하지 않습니다: " + sourcePath);
			return null;
		}

		File target = new File(targetPath);
		File parent = target.getParentFile();
		if (parent != null && !parent.exists()) {
			if (!parent.mkdirs()) {
				System.out.println("타겟 디렉터리 생성 실패: " + parent.getAbsolutePath());
				return null;
			}
		}

		try (FileInputStream fis = new FileInputStream(source); FileOutputStream fos = new FileOutputStream(target)) {

			byte[] buffer = new byte[8192];
			int len;
			while ((len = fis.read(buffer)) != -1) {
				fos.write(buffer, 0, len);
			}
			fos.flush();
			return target;
		} catch (IOException e) {
			System.out.println("파일 복사 중 오류 발생: " + sourcePath + " -> " + targetPath);
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 파일 또는 디렉터리를 삭제합니다.
	 * 디렉터리인 경우 내부 파일/하위 디렉터리까지 재귀적으로 모두 삭제합니다.
	 *
	 * @param path: 삭제할 파일 또는 디렉터리 경로
	 * @return 삭제에 성공하면 true, 실패하면 false 반환
	 */
	public static boolean deletePath(String path) {
	    if (path == null || path.isEmpty()) {
	        return false;
	    }

	    File target = new File(path);
	    return deleteRecursively(target);
	}
	

	/**
	 * deletePath에서 사용하는 재귀 삭제 메서드.
	 */
	private static boolean deleteRecursively(File file) {
	    if (!file.exists()) {
	        // 이미 존재하지 않으면 삭제된 것으로 간주
	        return true;
	    }

	    // file객체가 파일이 아니라 디렉토리라면 그 내부의 자식 요소들(파일이든 디렉토리든)을 재귀적으로 삭제 시도.
	    if (file.isDirectory()) {
	        File[] children = file.listFiles();
	        if (children != null) {
	            for (File child : children) {
	                if (!deleteRecursively(child)) {
	                    // 하나라도 삭제에 실패하면 전체 실패로 간주
	                    return false;
	                }
	            }
	        }
	    }

	    // 파일 또는 (비어 있게 된) 디렉터리 삭제 시도
	    // 자바에 내용물이 있는 디렉토리를 삭제해주는 기능이 없다..
	    // 재귀적으로 내부를 전부 비워주고 삭제를 시도해야한다...
	    return file.delete();
	}

}
