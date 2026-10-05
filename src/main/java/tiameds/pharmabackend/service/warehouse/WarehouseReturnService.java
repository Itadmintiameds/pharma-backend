package tiameds.pharmabackend.service.warehouse;

import tiameds.pharmabackend.dto.warehouse.WarehouseReturnDto;
import tiameds.pharmabackend.entity.UserDetails;

import java.util.List;

public interface WarehouseReturnService {

    // Raises a stock return from the user's selected pharmacy to a warehouse.
    WarehouseReturnDto createWarehouseReturn(WarehouseReturnDto warehouseReturnDto, UserDetails user);

    // A pharmacy sees the returns it raised; a warehouse sees the returns sent to it.
    List<WarehouseReturnDto> getAllWarehouseReturns(UserDetails user);

    // Same visibility as getAll: the sending pharmacy or the receiving warehouse.
    WarehouseReturnDto getWarehouseReturnById(Long warehouseReturnId, UserDetails user);

    // Sends a DRAFT return to the warehouse: every line is dispatched at its full
    // return quantity, taken out of the pharmacy's inventory, and the return moves
    // to PENDING_RECEIPT.
//    WarehouseReturnDto dispatchWarehouseReturn(Long warehouseReturnId, UserDetails user);

    // Records what the warehouse received against a Pending Receipt return:
    // per-line received / not received quantities and the header totals, then
    // moves the return to Complete.
    WarehouseReturnDto receiveWarehouseReturn(
            Long warehouseReturnId,
            WarehouseReturnDto warehouseReturnDto,
            UserDetails user);

    WarehouseReturnDto updateWarehouseReturn(
            Long warehouseReturnId,
            WarehouseReturnDto warehouseReturnDto,
            UserDetails user
    );
}
