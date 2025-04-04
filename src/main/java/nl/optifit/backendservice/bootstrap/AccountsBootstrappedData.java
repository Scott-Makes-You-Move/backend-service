package nl.optifit.backendservice.bootstrap;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AccountsBootstrappedData {
    private List<String> accountIds = new ArrayList<>();
    private int total;
}
