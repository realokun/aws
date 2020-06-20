package com.aws.vokunev.catalog.controller;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.aws.vokunev.catalog.data.Product;
import com.aws.vokunev.catalog.data.ProductDataAccessor;

@WebServlet("/product")
public class ProductDetailsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // This input paramater is validated by the filter
        int productId = Integer.parseInt(request.getParameter("id"));
        // Retrieve a product for the provided id
        Product product = ProductDataAccessor.getProduct(productId);
        // Make the model available to the view
        request.setAttribute("product", product);
        // Forward control to the view
        request.getRequestDispatcher("/WEB-INF/views/product.jsp").forward(request, response);
    }
}