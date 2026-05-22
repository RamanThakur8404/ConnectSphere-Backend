package com.connectsphere.payment_service.constant;

// Categories of payments supported by ConnectSphere.
public enum PaymentType {
    // Monthly / annual premium subscription 
    SUBSCRIPTION,

    // One-time fee to boost a post in the feed 
    BOOST_POST,

    // Virtual gift sent from one user to another 
    VIRTUAL_GIFT,

    // Payout from ConnectSphere to a creator 
    CREATOR_PAYOUT
}
