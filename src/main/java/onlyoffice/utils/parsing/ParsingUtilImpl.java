package onlyoffice.utils.parsing;

import javax.enterprise.inject.Default;
import javax.inject.Named;
import java.io.InputStream;
import java.util.Scanner;

@Named
@Default
public class ParsingUtilImpl implements ParsingUtil {
    public String getBody(final InputStream stream) {
        Scanner scanner = null;
        Scanner scannerUseDelimiter = null;
        try {
            scanner = new Scanner(stream);
            scannerUseDelimiter = scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } finally {
            scannerUseDelimiter.close();
            scanner.close();
        }
    }
}
