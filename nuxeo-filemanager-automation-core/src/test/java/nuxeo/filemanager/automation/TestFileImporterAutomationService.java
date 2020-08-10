package nuxeo.filemanager.automation;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class })
@Deploy("nuxeo.filemanager.automation.nuxeo-filemanager-automation-core")
public class TestFileImporterAutomationService {

    @Inject
    protected FileImporterAutomationService fileimporterautomation;

    @Test
    public void testService() {
        assertNotNull(fileimporterautomation);
    }
}
