package x.domainpersistencemodeling

import io.micronaut.runtime.Micronaut
import lombok.Generated
import java.time.ZoneOffset.UTC
import java.util.TimeZone

@Generated
object DemoApplication {
    @JvmStatic
    fun main(args: Array<String>) {
        // TODO: TZ.setDefault does not update sysprops.  Is it enough to
        //  update the sysprop without calling TZ.setDefault?
        System.setProperty("user.timezone", "UTC")
        TimeZone.setDefault(TimeZone.getTimeZone(UTC))

        Micronaut.build().start()
    }
}
