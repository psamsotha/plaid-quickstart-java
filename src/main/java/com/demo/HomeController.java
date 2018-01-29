package com.demo;

import com.plaid.client.PlaidClient;
import com.plaid.client.request.AuthGetRequest;
import com.plaid.client.request.InstitutionsGetByIdRequest;
import com.plaid.client.request.ItemGetRequest;
import com.plaid.client.request.ItemPublicTokenExchangeRequest;
import com.plaid.client.request.TransactionsGetRequest;
import com.plaid.client.response.AuthGetResponse;
import com.plaid.client.response.InstitutionsGetByIdResponse;
import com.plaid.client.response.ItemGetResponse;
import com.plaid.client.response.ItemPublicTokenExchangeResponse;
import com.plaid.client.response.ItemStatus;
import com.plaid.client.response.TransactionsGetResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import retrofit2.Response;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.*;


@Controller
public class HomeController {

    private final Environment env;
    private final PlaidClient plaidClient;
    private final PlaidAuthService authService;


    @Autowired
    public HomeController(Environment env, PlaidClient plaidClient, PlaidAuthService authService) {
        this.env = env;
        this.plaidClient = plaidClient;
        this.authService = authService;
    }


    /**
     * Home page.
     */
    @RequestMapping(value="/", method=GET)
    public String index(Model model) {
        model.addAttribute("PLAID_PUBLIC_KEY", env.getProperty("PLAID_PUBLIC_KEY"));
        model.addAttribute("PLAID_ENV", env.getProperty("PLAID_ENV"));
        return "index";
    }

    /**
     * Exchange link public token for access token.
     */
    @RequestMapping(value="/get_access_token", method=POST, consumes=MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public @ResponseBody ResponseEntity getAccessToken(@RequestParam("public_token") String publicToken) throws Exception {
        Response<ItemPublicTokenExchangeResponse> response = this.plaidClient.service()
                .itemPublicTokenExchange(new ItemPublicTokenExchangeRequest(publicToken))
                .execute();

        if (response.isSuccessful()) {
            this.authService.setAccessToken(response.body().getAccessToken());
            this.authService.setItemId(response.body().getItemId());

            Map<String, Object> data = new HashMap<>();
            data.put("error", false);

            return ResponseEntity.ok(data);
        } else {
            return ResponseEntity.status(500).body(getErrorResponseData(response.errorBody().string()));
        }
    }

    /**
     * Retrieve high-level account information and account and routing numbers
     * for each account associated with the Item.
     */
    @RequestMapping(value="/accounts", method=GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity getAccount() throws Exception {
        if (authService.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(getErrorResponseData("Not authorized"));
        }

        Response<AuthGetResponse> response = this.plaidClient.service()
                .authGet(new AuthGetRequest(this.authService.getAccessToken())).execute();

        if (response.isSuccessful()) {
            Map<String, Object> data = new HashMap<>();
            data.put("error", false);
            data.put("accounts", response.body().getAccounts());
            data.put("numbers", response.body().getNumbers());

            return ResponseEntity.ok(data);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(getErrorResponseData(response.errorBody().string()));
        }
    }

    /**
     * Pull the Item - this includes information about available products,
     * billed products, webhook information, and more.
     */
    @RequestMapping(value="/item", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity getItem() throws Exception {
        if (authService.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(getErrorResponseData("Not authorized"));
        }

        Response<ItemGetResponse> itemResponse = this.plaidClient.service()
                .itemGet(new ItemGetRequest(this.authService.getAccessToken()))
                .execute();
        if (!itemResponse.isSuccessful()) {
            Map<String, Object> data = new HashMap<>();
            data.put("error", itemResponse.errorBody());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(data);
        } else {
            ItemStatus item = itemResponse.body().getItem();

            Response<InstitutionsGetByIdResponse> institutionsResponse = this.plaidClient.service()
                    .institutionsGetById(new InstitutionsGetByIdRequest(item.getInstitutionId()))
                    .execute();

            if (!institutionsResponse.isSuccessful()) {
                Map<String, Object> data = new HashMap<>();
                data.put("error", itemResponse.errorBody());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(data);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("error", false);
                data.put("item", item);
                data.put("institution", institutionsResponse.body().getInstitution());
                return ResponseEntity.ok(data);
            }
        }
    }

    /**
     * Pull transactions for the Item for the last 30 days.
     */
    @RequestMapping(value="/transactions", method=POST, produces=MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity getTransactions() throws Exception {
        if (authService.getAccessToken() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(getErrorResponseData("Not authorized"));
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -30);
        Date startDate = cal.getTime();
        Date endDate = new Date();

        Response<TransactionsGetResponse> response = this.plaidClient.service()
                .transactionsGet(new TransactionsGetRequest(this.authService.getAccessToken(), startDate, endDate)
                        .withCount(250)
                        .withOffset(0))
                .execute();
        if (response.isSuccessful()) {
            return ResponseEntity.ok(response.body());
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("error", response.errorBody());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(data);
        }
    }

    private Map<String, Object> getErrorResponseData(String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("error", false);
        data.put("message", message);
        return data;
    }
}
