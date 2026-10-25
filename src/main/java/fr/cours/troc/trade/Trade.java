package fr.cours.troc.trade;

public class Trade {

    private Long id;
    private TradeStatus status;
    private Long proposerId;
    private Long offeredItemId;
    private int offeredQuantity;
    private Long recipientId;
    private Long requestedItemId;
    private int requestedQuantity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TradeStatus getStatus() {
        return status;
    }

    public void setStatus(TradeStatus status) {
        this.status = status;
    }

    public Long getProposerId() {
        return proposerId;
    }

    public void setProposerId(Long proposerId) {
        this.proposerId = proposerId;
    }

    public Long getOfferedItemId() {
        return offeredItemId;
    }

    public void setOfferedItemId(Long offeredItemId) {
        this.offeredItemId = offeredItemId;
    }

    public int getOfferedQuantity() {
        return offeredQuantity;
    }

    public void setOfferedQuantity(int offeredQuantity) {
        this.offeredQuantity = offeredQuantity;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public Long getRequestedItemId() {
        return requestedItemId;
    }

    public void setRequestedItemId(Long requestedItemId) {
        this.requestedItemId = requestedItemId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(int requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }
}
