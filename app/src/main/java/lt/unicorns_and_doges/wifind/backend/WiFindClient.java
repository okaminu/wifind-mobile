package lt.unicorns_and_doges.wifind.backend;

import com.amazonaws.mobileconnectors.apigateway.annotation.Operation;
import com.amazonaws.mobileconnectors.apigateway.annotation.Service;

import lt.unicorns_and_doges.wifind.model.WifiSpot;

@Service(endpoint = "http://wifind.tripolis.us")
public interface WiFindClient {

    /**
     *
     *
     * @return Empty
     */
    @Operation(path = "/save", method = "POST")
    boolean save(WifiSpot wifiSpot);

    /**
     *
     *
     * @return Empty
     */
    @Operation(path = "/get-all", method = "GET")
    WifiSpot[] getAll();

}





