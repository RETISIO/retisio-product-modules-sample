package com.retisio.arc.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import play.libs.Files;
import play.mvc.Http;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class CsvUtil {

    public static <T> List<T> getRecords(Http.MultipartFormData.FilePart<Files.TemporaryFile> filePart, Class<T> classType) {

        CsvMapper csvMapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader().withColumnSeparator(',');
        ObjectReader oReader = csvMapper.readerFor(classType).with(schema);
        BufferedReader bufferedReader = null;
        InputStreamReader inputStreamReader = null;
        ByteArrayInputStream byteArrayInputStream = null;
        InputStream fileInputStream = null;
        List<T> list = null;
        try {

            File file = filePart.getRef().path().toFile();
            fileInputStream = new FileInputStream(file);
            byteArrayInputStream = new ByteArrayInputStream(IOUtils.toByteArray(fileInputStream));
            inputStreamReader = new InputStreamReader(byteArrayInputStream,"UTF-8");
            bufferedReader = new BufferedReader(inputStreamReader);

            if (bufferedReader != null) {
                try (Reader reader = bufferedReader) {
                    MappingIterator<T> mi = oReader.readValues(reader);
                    list = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(mi, Spliterator.ORDERED), true)
                            .collect(Collectors.toList());

                    log.info("JACKSON_PARSE_CSV_ROW_SIZE {} ", list.size());
                } catch (IOException e) {
                    log.error("[IOException]", e);
                }
            }
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            log.error("[UnsupportedEncodingException]", e);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("IOEXCEPTION_OCCURRED_WHILE_PARSING_THE_PRICE_CSV_FILE_DURING_IMPORT_PROCESS", e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                    inputStreamReader.close();
                    byteArrayInputStream.close();
                    fileInputStream.close();
                }
            } catch (Exception e) {
                log.error("Excepiton", e);
            }
        }
        return list;
    }

    //String tempFolder = "c:/tmp/";
    //String fileName = "catalog_output.csv";
/*    List<String> columns = Arrays.asList(
            "priceGroupID",
            "productID",
            "skuID",
            "listPrice",
            "salePrice",
            "surcharge"
    );*/
    public static <T> byte[] getBytes(List<T> list, String tempFolder, String fileName, List<String> columns, Class<T> classType){

        createDirectories(tempFolder);
        String filePath = tempFolder+fileName;
        File csvOutputFile = new File(filePath);
        CsvMapper mapper = new CsvMapper();
        mapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        schemaBuilder.setUseHeader(true);
        columns.forEach(c -> {
            schemaBuilder.addColumn(c);
        });

        CsvSchema schema = schemaBuilder.build();
        ObjectWriter writer = mapper.writerFor(classType).with(schema);
        InputStream fileInputStream = null;
        try {
            writer.writeValues(csvOutputFile).writeAll(list);
            fileInputStream = new FileInputStream(csvOutputFile);
            return IOUtils.toByteArray(fileInputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (Exception e) {
                //ignore
            }
        }
        return "Empty File".getBytes();
    }
    private static void createDirectories(String folderPath) {
        try {
            Path projectPath = Paths.get(folderPath);
            java.nio.file.Files.createDirectories(projectPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
