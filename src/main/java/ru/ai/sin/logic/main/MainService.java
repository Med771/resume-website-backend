package ru.ai.sin.logic.main;

public interface MainService {
    byte[] getFileContent(String fileName);

    String getContentType(String imagePath);
}
