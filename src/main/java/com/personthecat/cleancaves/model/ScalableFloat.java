package com.personthecat.cleancaves.model;

/**
 * Describes how a particular floating point value is intended to change throughout its lifetime.
 */
public record ScalableFloat(float startVal, float startValRandFactor, float factor, float randFactor, float exponent)
{ }
