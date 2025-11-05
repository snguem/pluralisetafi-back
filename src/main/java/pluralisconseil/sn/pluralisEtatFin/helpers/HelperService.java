package pluralisconseil.sn.pluralisEtatFin.helpers;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pluralisconseil.sn.pluralisEtatFin.exceptions.NotFoundException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HelperService  {
    private final String FOLDER_NAME = "public";
    private final String TEMP_FOLDER = "public/temps";
    private final List<String> FOLDERS = List.of("excels", "imgs", "pdfs");

    public String getExtensionFile(MultipartFile file){
        return Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf('.') + 1);
    }

    public String saveFile(int folderNum, String fileName, File fileToSave){
        try {
            Path directory = Paths.get(String.join("/", List.of(FOLDER_NAME, FOLDERS.get(folderNum))));
            Path filepath = Paths.get(String.join("/", List.of(FOLDER_NAME, FOLDERS.get(folderNum), fileName)));
            if (!Files.exists(directory)){
                Files.createDirectory(directory);
            }
            Files.copy(fileToSave.toPath(),filepath);
            return filepath.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String saveFile(int folder, String prefix, MultipartFile fileToTemp){
        String name;
        try {
            isMainFolderExist();

            String FOLDER = FOLDERS.get(folder);
            name = uniqueName(prefix, getExtensionFile(fileToTemp));
            Path directory = Paths.get(String.join("/", List.of(FOLDER_NAME, FOLDER)));
            Path filepath = Paths.get(String.join("/", List.of(FOLDER_NAME, FOLDER, name)));
            if (!Files.exists(directory)){
                Files.createDirectory(directory);
            }
            Files.write(filepath, fileToTemp.getBytes());
            return String.join("/", FOLDER_NAME, FOLDER, name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String uniqueName(String startName, String extension) {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString();
        return startName+"_"+uuid + "_" + timestamp + "." + extension;
    }


    public String saveTempFile(MultipartFile fileToTemp){
        String name;
        try {

            isMainFolderExist();

            name = uniqueName("temp", getExtensionFile(fileToTemp));
            Path directory = Paths.get(TEMP_FOLDER);
            Path filepath = Paths.get(String.join("/", List.of(TEMP_FOLDER, name)));
            if (!Files.exists(directory)){
                Files.createDirectory(directory);
            }
            Files.write(filepath, fileToTemp.getBytes());
            return String.join("/", TEMP_FOLDER, name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String saveTempFile(byte[] bytes){
        String name;
        try {
            name = uniqueName("temp", "pdf");
            Path directory = Paths.get(TEMP_FOLDER);
            Path filepath = Paths.get(String.join("/", List.of(TEMP_FOLDER, name)));
            if (!Files.exists(directory)){
                Files.createDirectory(directory);
            }

            try (FileOutputStream fos = new FileOutputStream(filepath.toFile().getPath())) {
                fos.write(bytes);
            }
            return filepath.toFile().getPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean removesTemp(List<String> filesTemps){
        for (String fileName:filesTemps){
            remove(fileName);
        }
        return true;
    }

    public boolean removesTemp(String fileTemp){
        File file=new File(fileTemp);
        return file.delete();
    }
    public boolean remove(String file_name){
        try {
            Path path = Paths.get(file_name);
            if (Files.exists(path)){
                File file=new File(file_name);
                return file.delete();
            }
        }catch (Exception e){
            return false;
        }
        return false;
    }


    public void isMainFolderExist() throws IOException {
        Path directory = Paths.get(FOLDER_NAME);
        if (!Files.exists(directory)){
            Files.createDirectory(directory);
        }
    }


    public boolean isExist(String path) throws IOException {
        if (path==null) throw new NotFoundException("Fichier inexistant");
        File file = new File(path);
        return file.exists() && file.isFile();
    }



    public void deleteFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        boolean deleted = file.delete();
                    }
                }
            }
        }
    }
}
