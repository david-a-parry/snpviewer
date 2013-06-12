

package snpviewer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public class LineCount {
       public static int count(File file) throws IOException {
           try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                byte[] c = new byte[1024];
                int count = 0;
                int readChars = 0; 
                boolean empty = true;
                while ((readChars = is.read(c)) != -1) {
                    empty = false;
                    for (int i = 0; i < readChars; ++i) {
                        if (c[i] == '\n') {
                            ++count;
                        }
                    }
                }
                return (count == 0 && !empty) ? 1 : count;
            }
        }
}

