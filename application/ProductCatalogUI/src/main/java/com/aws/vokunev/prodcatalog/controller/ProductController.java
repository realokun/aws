package com.aws.vokunev.prodcatalog.controller;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.aws.vokunev.prodcatalog.dao.ProductAccessor;
import com.aws.vokunev.prodcatalog.model.AppConfig;
import com.aws.vokunev.prodcatalog.model.AppPermissions;
import com.aws.vokunev.prodcatalog.model.CatalogItem;
import com.aws.vokunev.prodcatalog.model.Product;
import com.aws.vokunev.prodcatalog.model.RequestScopeConfig;
import com.aws.vokunev.prodcatalog.util.CorrelatingLogger;

/**
 * This controller handles requests specific to the Product Catalog business
 * domain.
 */
@Controller
public class ProductController {

    @Autowired
    private RequestScopeConfig requestScope;
    @Autowired
    private ProductAccessor productDataAccessor;
    @Autowired
    private CorrelatingLogger logger;

    @PostConstruct
    private void init() {
        logger.init(ProductController.class);
    }

    @GetMapping("/")
    public String productList(Model model)
            throws Exception {

        logger.info("Requesting the product list...");

        // retrieve the product list
        AppConfig appConfig = requestScope.getAppConfig();
        List<CatalogItem> catalog = productDataAccessor.getProductCatalog(appConfig.getServiceEndpointProductList(),
                appConfig.getApiKey());
        logger.info(String.format("Retrieved %d product items", catalog.size()));

        initGetRequestModel(model);

        // make the product list available to the view
        model.addAttribute("catalog", catalog);

        // return the name of the view file
        return "product_list";
    }

    @GetMapping("/product")
    public String productDetails(@RequestParam(name = "id", required = true) int productId,
            @RequestParam(name = "newPrice", required = false) String newPrice, Model model) throws Exception {

        logger.info(String.format("Requesting product details for product id=%d", productId));

        // retrieve a product by the provided id
        AppConfig appConfig = requestScope.getAppConfig();
        Product product = productDataAccessor.getProduct(appConfig.getServiceEndpointProductDetails(),
                appConfig.getApiKey(),
                productId);
        logger.info(String.format("Retrieved product details: %s", product));

        initGetRequestModel(model);

        // make the product available to the view
        model.addAttribute("product", product);

        // provide indication to the view that the price update has been requested
        model.addAttribute("newPrice", newPrice);

        // return the name of the view file
        return "product_details";
    }

    @PostMapping("/updatePrice")
    public ModelAndView productPriceUpdate(Product product, ModelMap model) throws Exception {

        logger.info(String.format("Submitting price update for %s", product));

        // send product price update request
        AppConfig appConfig = requestScope.getAppConfig();
        productDataAccessor.updatePrice(appConfig.getServiceEndpointProductPriceUpdate(), appConfig.getApiKey(),
                product.getId(), product.getPrice());

        // redirect to the product details page
        model.addAttribute("id", product.getId());
        model.addAttribute("newPrice", product.getPrice());
        return new ModelAndView("redirect:/product", model);
    }

    /**
     * This method performs some common model initialization tasks for a GET
     * request.
     */
    private void initGetRequestModel(Model model) {

        // make the application configuration available to the view
        model.addAttribute("config", requestScope.getAppConfig());

        // make the auth token available to the view
        model.addAttribute("token", requestScope.getAccessToken());

        // make the instance metadata available to the view
        model.addAttribute("metadata", requestScope.getMetadata());

        // initialize application permissions based on the combination of the access
        // token and the application configuration data
        AppPermissions permissions = new AppPermissions(requestScope.getAccessToken(), requestScope.getAppConfig());

        // make the application permissions available to the view
        model.addAttribute("permissions", permissions);
    }
}
