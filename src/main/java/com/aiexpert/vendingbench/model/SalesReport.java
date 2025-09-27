package com.aiexpert.vendingbench.model;

/**
 * A simple record to hold the results of a day's sales simulation.
 * This makes it easy to pass sales data between different parts of the engine.
 *
 * @param unitsSold The total number of units sold during the day.
 * @param revenue   The total revenue generated from sales.
 * @param details   A string containing a detailed breakdown of sales by item.
 */
public record SalesReport(int unitsSold, double revenue, String details) {
}