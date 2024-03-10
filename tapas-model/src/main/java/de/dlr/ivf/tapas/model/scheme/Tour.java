package de.dlr.ivf.tapas.model.scheme;

import lombok.AllArgsConstructor;

import java.util.Collection;

@AllArgsConstructor
public class Tour {

    private final Collection<Trip> trips;
}
