import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("Hello, world!");
            System.out.println("Please enter the path of the directory containing the files to patch in:");
            Scanner scanner = new Scanner(System.in);
            String patchFolderPath = scanner.nextLine();
            if (!Files.exists(Path.of(patchFolderPath))) {
                throw new Exception("The folder (" + patchFolderPath + ") could not be found!");
            }

            List<String> foldersToCheck = new ArrayList<>();
            List<String> smaliFiles = new ArrayList<>();
            foldersToCheck.add(patchFolderPath);
            while(foldersToCheck.size() > 0) {
                String folderToCheck = foldersToCheck.remove(0);
                File tempFile = new File(folderToCheck);
                String[] directories = tempFile.list((current, name) -> new File(current, name).isDirectory());
                String[] files = tempFile.list((current, name) -> new File(current, name).isFile());
                if(directories != null) {
                    for (String directory : directories) {
                        foldersToCheck.add(folderToCheck + "/" + directory);
                    }
                }
                if(files != null) {
                    for (String file : files) {
                        if (file.endsWith(".smali")) {
                            smaliFiles.add(folderToCheck + "/" + file);
                        }
                    }
                }
            }

            System.out.println(smaliFiles.size() + " .smali files to patch!");

            List<String> classes = new ArrayList<>();
            for(String filePath : smaliFiles) {
                File readFile = new File(filePath);
                Scanner fileReader = new Scanner(readFile);
                while(fileReader.hasNextLine()) {
                    String line = fileReader.nextLine();
                    if(line.startsWith(".class")) {
                        int lIndex = line.indexOf('L');
                        if(lIndex > 0) {
                            String className = line.substring(lIndex);
                            classes.add(className);
                        }
                    }
                }
                fileReader.close();
            }

            System.out.println(classes.size() + " classes to patch.");

            int patchedLineCount = 0;
            List<String> patchedFilePaths = new ArrayList<>(classes.size());
            for(String filePath : smaliFiles) {
                String patchedFilePath = filePath;

                int prefixIndex = filePath.lastIndexOf('/') + 1;
                int suffixIndex = filePath.lastIndexOf('.');
                if(prefixIndex >= 0 && suffixIndex >= 0) {
                    patchedFilePath = filePath.substring(0, suffixIndex);
                    patchedFilePath += "Patched.smali";
                }

                File readFile = new File(filePath);
                Scanner fileReader = new Scanner(readFile);
                List<String> newLines = new ArrayList<>();
                while(fileReader.hasNextLine()) {
                    String line = fileReader.nextLine();

                    String finalLine = line;
                    List<String> foundClasses = classes.stream().filter(item -> finalLine.contains(item)).collect(Collectors.toList());
                    for(String foundClass : foundClasses) {
                        String prefixClass = foundClass.substring(0, foundClass.length() - 1);
                        String patchedClass = prefixClass + "Patched;";
                        line = line.replace(foundClass, patchedClass);
                        patchedLineCount += 1;
                    }

                    newLines.add(line);
                }
                fileReader.close();

                File patchedFile = new File(patchedFilePath);
                patchedFile.createNewFile();
                FileWriter fileWriter = new FileWriter(patchedFile);
                for(String line : newLines) {
                    fileWriter.write(line);
                }
                fileWriter.close();
            }
        } catch(Exception exc) {
            exc.printStackTrace();
        }
    }
}
