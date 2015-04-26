package ut.eu.ganymede.jira.cardsPrinter;

import org.junit.Test;
import eu.ganymede.jira.cardsPrinter.MyPluginComponent;
import eu.ganymede.jira.cardsPrinter.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}