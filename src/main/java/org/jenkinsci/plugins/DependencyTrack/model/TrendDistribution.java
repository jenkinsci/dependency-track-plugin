package org.jenkinsci.plugins.DependencyTrack.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@RequiredArgsConstructor(staticName = "of")
@Getter
public class TrendDistribution implements Serializable
{
  private static final long serialVersionUID = 5185577162525878522L;

  private final int buildNumber;
  private int critical;
  private int high;
  private int medium;
  private int low;
  private int info;
  private int unassigned;
  private int fail;
  private int warn;

  public TrendDistribution addCritical(final int _critical)
  {
    critical += _critical;

    return this;
  }

  public TrendDistribution addHigh(final int _high)
  {
    high += _high;

    return this;
  }

  public TrendDistribution addMedium(final int _medium)
  {
    medium += _medium;

    return this;
  }

  public TrendDistribution addLow(final int _low)
  {
    low += _low;

    return this;
  }

  public TrendDistribution addInfo(final int _info)
  {
    info += _info;

    return this;
  }

  public TrendDistribution addUnassigned(final int _unassigned)
  {
    unassigned += _unassigned;

    return this;
  }

  public TrendDistribution addFail(final int _fail)
  {
    fail += _fail;

    return this;
  }

  public TrendDistribution addWarn(final int _warn)
  {
    warn += _warn;

    return this;
  }
}
