package util;

import main.storage.Storage;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

public class CsvUtil {
  public static HashSet<String> getRecordSet(Path in, int[] care, boolean hasHeader) {
    HashSet<String> hs = new HashSet<>();
    try {
      BufferedReader br = new BufferedReader(new FileReader(in.toFile()));
      Iterable<CSVRecord> records = CSVFormat.DEFAULT.withSkipHeaderRecord(hasHeader).parse(br);
      for (CSVRecord record : records) {
        String[] tmp = new String[care.length];
        for (int i = 0; i < care.length; i++) {
          tmp[i] = record.get(care[i]);
        }
        hs.add(String.join(",", tmp));
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
    return hs;
  }

  public static void compareTwoCsv(Path in1, Path in2, Path out, int[] care1, int[] care2) {
    if (care1.length != care2.length) return;
    try {
      HashSet<String> records1 = getRecordSet(in1, care1, true);
      HashSet<String> records2 = getRecordSet(in2, care2, false);
      HashSet<String> diff12 = new HashSet<>(records1);
      diff12.removeAll(records2);
      HashSet<String> diff21 = new HashSet<>(records2);
      diff21.removeAll(records1);
      MemorizeUtil.createDirIfAbsent(out.getParent().toFile());
      BufferedWriter bw = new BufferedWriter(new FileWriter(out.toFile()));
      for (String tmp : diff12) {
        bw.write(tmp + "\n");
      }
      bw.write("null,null\n");
      for (String tmp : diff21) {
        bw.write(tmp + "\n");
      }
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
