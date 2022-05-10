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
            System.out.println("Please enter the path of the directory containing the files to patch in:");
            Scanner scanner = new Scanner(System.in);
            String patchFolderPath = scanner.nextLine();
            if (!Files.exists(Path.of(patchFolderPath))) {
                throw new Exception("The folder (" + patchFolderPath + ") could not be found!");
            }

            /*
            Get all the .smali files from any of the subdirectories.
            */
            List<String> foldersToCheck = new ArrayList<>();
            List<String> smaliFiles = new ArrayList<>();
            foldersToCheck.add(patchFolderPath);
            while (foldersToCheck.size() > 0) {
                String folderToCheck = foldersToCheck.remove(0);
                File tempFile = new File(folderToCheck);
                String[] directories = tempFile.list((current, name) -> new File(current, name).isDirectory());
                String[] files = tempFile.list((current, name) -> new File(current, name).isFile());
                if (directories != null) {
                    for (String directory : directories) {
                        foldersToCheck.add(folderToCheck + File.separatorChar + directory);
                    }
                }
                if (files != null) {
                    for (String file : files) {
                        if (file.endsWith(".smali")) {
                            smaliFiles.add(folderToCheck + File.separatorChar + file);
                        }
                    }
                }
            }

            System.out.println(smaliFiles.size() + " .smali files to patch!");

            /*
            Extract the class names (and packages) from the .smali files.
             */
            List<String> classes = new ArrayList<>();
            for (String filePath : smaliFiles) {
                File readFile = new File(filePath);
                Scanner fileReader = new Scanner(readFile);
                while (fileReader.hasNextLine()) {
                    String line = fileReader.nextLine();
                    if (line.startsWith(".class")) {
                        int lIndex = line.indexOf('L');
                        if (lIndex > 0) {
                            String className = line.substring(lIndex);
                            classes.add(className);
                        }
                    }
                }
                fileReader.close();
            }

            System.out.println(classes.size() + " classes to patch.");

            /*
            Patch the files.
             */
            int patchedLineCount = 0;
            List<String> patchedFilePaths = new ArrayList<>(classes.size());
            for (String filePath : smaliFiles) {
                String patchedFilePath = filePath;

                int prefixIndex = filePath.lastIndexOf(File.separatorChar) + 1;
                int suffixIndex = filePath.lastIndexOf('.');
                if (prefixIndex >= 1 && suffixIndex >= 0) {
                    patchedFilePath = filePath.substring(0, suffixIndex);
                    patchedFilePath += "Patched.smali";
                }

                File readFile = new File(filePath);
                Scanner fileReader = new Scanner(readFile);
                List<String> newLines = new ArrayList<>();
                while (fileReader.hasNextLine()) {
                    String line = fileReader.nextLine();

                    String finalLine = line;
                    List<String> foundClasses = classes.stream().filter(finalLine::contains).toList();
                    for (String foundClass : foundClasses) {
                        String prefixClass = foundClass.substring(0, foundClass.length() - 1);
                        String patchedClass = prefixClass + "Patched;";
                        line = line.replace(foundClass, patchedClass);
                        patchedLineCount += 1;
                    }

                    newLines.add(line);
                }
                fileReader.close();

                File patchedFile = new File(patchedFilePath);
                if(!patchedFile.createNewFile()){
                    throw new Exception(patchedFilePath + " already exists!");
                }

                FileWriter fileWriter = new FileWriter(patchedFile);
                for (String line : newLines) {
                    fileWriter.write(line + "\n");
                }
                fileWriter.close();

                patchedFilePaths.add(patchedFile.getAbsolutePath());
            }

            System.out.println(patchedLineCount + " lines have been altered.");

            System.out.println("Please enter the directory to patch the files into:");
            String writeDirectoryPath = scanner.nextLine();
            if (writeDirectoryPath == null || !Files.exists(Path.of(writeDirectoryPath))) {
                throw new Exception("Invalid directory given!");
            }

            /*
            Get the amount of smali folders in the target directory
             */
            int smaliCount = 0;
            File tempFile = new File(writeDirectoryPath);
            String[] directories = tempFile.list((current, name) -> new File(current, name).isDirectory());
            if(directories == null || directories.length <= 0) {
                throw new Exception("No smali directories found!");
            }

            for (String dir : directories) {
                if (dir.contains("smali")) {
                    smaliCount += 1;
                }
            }

            /*
            Copy the patched files to the target directory.
             */
            for (String patchedFilePath : patchedFilePaths) {
                String fileName = patchedFilePath.substring(patchFolderPath.length() + 1);
                int endSmaliIndex = fileName.indexOf(File.separatorChar);
                String tempSmali = fileName.substring(0, endSmaliIndex);
                int numberIndex = tempSmali.indexOf('_');
                int smaliCounterModifier = 1;
                if (numberIndex >= 0) {
                    smaliCounterModifier = Integer.parseInt(tempSmali.substring(13));
                }

                int subDirIndex = fileName.lastIndexOf(File.separatorChar);
                String subDir = fileName.substring(0, subDirIndex);
                int smaliIndex = subDir.indexOf(File.separatorChar);
                String realSubDir = subDir.substring(smaliIndex);

                String newDir = writeDirectoryPath + File.separatorChar + "smali_classes" + (smaliCount + smaliCounterModifier) + realSubDir;
                Path newDirPath = Path.of(newDir);
                if (!Files.exists(newDirPath)) {
                    Files.createDirectories(newDirPath);
                }

                int lastSlashIndex = fileName.lastIndexOf(File.separatorChar);
                Path patchedFilePathPath = Path.of(patchedFilePath);
                if (lastSlashIndex > 0) {
                    String shortFileName = fileName.substring(lastSlashIndex);
                    Files.copy(patchedFilePathPath, Path.of(newDir + shortFileName));
                    Files.delete(patchedFilePathPath);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }
}
