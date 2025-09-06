package com.pagerealm.shoppingcart.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class RemoveItemFromCartRequest {
    private List<Long> ids;
}
