package com.abhijit.inventory_service.product;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ProductService productService;
    @InjectMocks private ReservationService reservationService;

    private final List<StockItem> items = List.of(new StockItem("SKU-1001", 2));

    private Reservation reservation(ReservationStatus status) {
        Reservation r = new Reservation("ORD-1", List.of(new ReservedItem("SKU-1001", 2)));
        if (status == ReservationStatus.RELEASED) r.markReleased();
        return r;
    }

    @Test
    void firstReserveReservesStockAndStoresReservation() {
        when(reservationRepository.findByOrderNumber("ORD-1")).thenReturn(Optional.empty());

        reservationService.reserve("ORD-1", items);

        verify(productService).reserve(items);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void repeatedReserveWithSameItemsDoesNothing() {
        when(reservationRepository.findByOrderNumber("ORD-1"))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED)));

        reservationService.reserve("ORD-1", items);

        verify(productService, never()).reserve(anyList());
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void reserveWithDifferentItemsIsRejected() {
        when(reservationRepository.findByOrderNumber("ORD-1"))
                .thenReturn(Optional.of(reservation(ReservationStatus.RESERVED)));

        assertThrows(ReservationConflictException.class,
                () -> reservationService.reserve("ORD-1", List.of(new StockItem("SKU-1001", 5))));
    }

    @Test
    void reserveAfterReleaseIsRejected() {
        when(reservationRepository.findByOrderNumber("ORD-1"))
                .thenReturn(Optional.of(reservation(ReservationStatus.RELEASED)));

        assertThrows(ReservationConflictException.class, () -> reservationService.reserve("ORD-1", items));
    }

    @Test
    void releaseGivesBackTheStoredItemsAndMarksReleased() {
        Reservation stored = reservation(ReservationStatus.RESERVED);
        when(reservationRepository.findByOrderNumber("ORD-1")).thenReturn(Optional.of(stored));

        reservationService.release("ORD-1");

        verify(productService).release(List.of(new StockItem("SKU-1001", 2)));
        assertEquals(ReservationStatus.RELEASED, stored.getStatus());
    }

    @Test
    void releasingTwiceDoesNothingTheSecondTime() {
        when(reservationRepository.findByOrderNumber("ORD-1"))
                .thenReturn(Optional.of(reservation(ReservationStatus.RELEASED)));

        reservationService.release("ORD-1");

        verify(productService, never()).release(anyList());
    }

    @Test
    void releaseOfUnknownOrderThrowsNotFound() {
        when(reservationRepository.findByOrderNumber("ORD-9")).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () -> reservationService.release("ORD-9"));
    }
}
