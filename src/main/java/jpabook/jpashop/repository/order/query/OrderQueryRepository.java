package jpabook.jpashop.repository.order.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    public List<OrderQueryDto> findOrderQueryDtos() {   // N+1 문제가 발생!
        List<OrderQueryDto> result = findOrders();  //query 1번 -> 2개(N개)
        result.forEach(o ->{
            List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId());    //Query 2번(N번)
            o.setOrderItems(orderItems);
        });
        return result;
    }

    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count) " +
                        " from OrderItem oi" +
                        " join oi.item i" +
                        " where oi.order.id = :orderId", OrderItemQueryDto.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    private List<OrderQueryDto> findOrders() {
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.query.OrderQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                                " from Order o" +
                                " join o.member m" +
                                " join o.delivery d", OrderQueryDto.class)
                .getResultList();
    }

    /**
     * 최적화
     * Query: 루트 1번, 컬렉션 1번
     * 데이터를 한꺼번에 처리할 때 많이 사용하는 방식
     *
     */
    public List<OrderQueryDto> findAllByDto_optimization() {

        //루트 조회(toOne 코드를 모두 한번에 조회)
        List<OrderQueryDto> result = findOrders();  // query 발생.

//        List<Long> orderIds = toOrderIds(result);
//        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(orderIds);

        //orderItem 컬렉션을 MAP 한방에 조회
        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(toOrderIds(result)); //코드 리팩토링

        //루프를 돌면서 컬렉션 추가(추가 쿼리 실행X)
        result.forEach(o-> o.setOrderItems(orderItemMap.get(o.getOrderId())));  // 메모리 맵에 올린 데이터를 루프로 뽑는다.

        return result;
    }

//    Query: 루트 1번, 컬렉션 1번
//    ToOne 관계들을 먼저 조회하고, 여기서 얻은 식별자 orderId로 ToMany 관계인 OrderItem 을
//    한꺼번에 조회
//    MAP을 사용해서 매칭 성능 향상(O(1))

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {  //JPQL을 직접적으로 DTO로 하는 것이 마냥 편하지 않는 것 같음(많은 코드를 직접 작성).
        List<OrderItemQueryDto> orderItems = em.createQuery(    // query 발생
                        "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto(oi.order.id, i.name, oi.orderPrice, oi.count) " +
                                " from OrderItem oi" +
                                " join oi.item i" +
                                " where oi.order.id in :orderIds", OrderItemQueryDto.class)
                .setParameter("orderIds", orderIds)
                .getResultList();

        Map<Long, List<OrderItemQueryDto>> orderItemMap = orderItems.stream()    // key: OrderItemQueryDto::getOrderId, 값: List<OrderItemQueryDto>
                .collect(Collectors.groupingBy(OrderItemQueryDto::getOrderId)); // 메모리 맵에 데이터를 올리고.
        return orderItemMap;
    }

    private static List<Long> toOrderIds(List<OrderQueryDto> result) {
        List<Long> orderIds = result.stream()   // orderIds의 리스트가 됨.
                .map(o -> o.getOrderId())
                .collect(Collectors.toList());
        return orderIds;
    }

    // findAllByDto_flat() 장점은 쿼리가 1번만 나간다.
//    단점
//    쿼리는 한번이지만 조인으로 인해 DB에서 애플리케이션에 전달하는 데이터에 중복 데이터가
//    추가되므로 상황에 따라 V5 보다 더 느릴 수 도 있다.
//    애플리케이션에서 추가 작업이 크다.

    public List<OrderFlatDto> findAllByDto_flat() { // 일대다 조인을 했기 때문에 데이터가 중복되어서 나갈 수 밖에 없다.
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.query.OrderFlatDto(o.id, m.name, o.orderDate, o.status, d.address, i.name, oi.orderPrice, oi.count)" +
                        " from Order o" +
                        " join o.member m" +
                        " join o.delivery d" +
                        " join o.orderItems oi " +
                        " join oi.item i", OrderFlatDto.class)
                .getResultList();
    }


}
