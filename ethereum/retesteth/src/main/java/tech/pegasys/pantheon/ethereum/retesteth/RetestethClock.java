package tech.pegasys.pantheon.ethereum.retesteth;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

public class RetestethClock extends Clock {

  private Optional<Instant> fixedInstant;
  private final Clock delegateClock;

  RetestethClock() {
    this(Clock.systemUTC());
  }

  private RetestethClock(final Clock delegateClock) {
    fixedInstant = Optional.empty();
    this.delegateClock = delegateClock;
  }

  @Override
  public ZoneId getZone() {
    return delegateClock.getZone();
  }

  @Override
  public Clock withZone(final ZoneId zone) {
    final RetestethClock zonedClock = new RetestethClock(delegateClock.withZone(zone));
    zonedClock.fixedInstant = fixedInstant;
    return zonedClock;
  }

  @Override
  public Instant instant() {
    return fixedInstant.orElseGet(delegateClock::instant);
  }

  public void resetTime(final long time) {
    fixedInstant = Optional.of(Instant.ofEpochSecond(time));
  }

  public void tickSeconds(final long seconds) {
    fixedInstant = Optional.of(Instant.ofEpochSecond(instant().getEpochSecond() + seconds));
  }
}
