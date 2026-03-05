package jsdp;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import jsdp.sdp.StateActionTest;
import jsdp.sdp.ValueRepositoryTest;
import jsdp.utilities.hash.MapDBHashTableTest;
import jsdp.utilities.probdist.DiscreteDistributionFactoryTest;

@RunWith(Suite.class)
@SuiteClasses({
        jsdp.utilities.sampling.AllTests.class,
        MapDBHashTableTest.class,
        DiscreteDistributionFactoryTest.class,
        StateActionTest.class,
        ValueRepositoryTest.class
})

public class AllTests {
    // This class remains empty, it is used only as a holder for the above
    // annotations
}
