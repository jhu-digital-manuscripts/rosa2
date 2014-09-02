package rosa.archive.core;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.config.AppConfig;

import java.util.Arrays;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class PropertyInjectionTest {

    @Inject
    private AppConfig context;

    @Test
    public void test() {
        System.out.println(context.getCHARSET());
        System.out.println(Arrays.toString(context.languages()));
    }

}
