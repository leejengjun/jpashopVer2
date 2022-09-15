package jpabook.jpashop.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.domain.QMember;
import jpabook.jpashop.domain.QOrder;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleOueryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

import static jpabook.jpashop.domain.QMember.member;
import static jpabook.jpashop.domain.QOrder.order;

@Repository
public class OrderRepository {

    private final EntityManager em;
    private final JPAQueryFactory query;

    public OrderRepository(EntityManager em) {
        this.em = em;
        this.query = new JPAQueryFactory(em);
    }

    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

//    public List<Order> findAll(OrderSearch orerSearch) {
//
//       return em.createQuery("select o from Order o join o.member m" +
//                        " where o.status = :status " +
//                        " and m.name like :name", Order.class)
//                .setParameter("status", orerSearch.getOrderStatus())
//                .setParameter("name", orerSearch.getMemberName())
//                .setMaxResults(1000)    //최대 1000건
//                .getResultList();
//    }

    public List<Order> findAllByString(OrderSearch orderSearch) {
        //language=JPAQL
        String jpql = "select o From Order o join o.member m";
        boolean isFirstCondition = true;
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        }
        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                .setMaxResults(1000); //최대 1000건
        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }
        return query.getResultList();
    }

    public List<Order> findAllWithMemberDelivery() {    // 재사용성이 높다.
        return em.createQuery(
                " select  o from Order o " +
                        " join fetch o.member m" +
                        " join fetch o.delivery d ", Order.class
        ).getResultList();
    }

    public List<OrderSimpleOueryDto> findOrderDtos() {  // 재사용은 어렵고 거의 1회용이라 생각하면 됨. / 화면에는 최적화
         return em.createQuery(
                "select new jpabook.jpashop.repository.order.simplequery.OrderSimpleOueryDto(o.id, m.name, o.orderDate, o.status, d.address) " +
                        " from Order o" +
                        " join o.member m" +
                        " join o.delivery d", OrderSimpleOueryDto.class)
                .getResultList();
    }

//
//    쿼리 방식 선택 권장 순서
//    1. 우선 엔티티를 DTO로 변환하는 방법을 선택한다.
//    2. 필요하면 페치 조인으로 성능을 최적화 한다. 대부분의 성능 이슈가 해결된다.
//    3. 그래도 안되면 DTO로 직접 조회하는 방법을 사용한다.
//    4. 최후의 방법은 JPA가 제공하는 네이티브 SQL이나 스프링 JDBC Template을 사용해서 SQL을 직접사용한다.

    public List<Order> findAllWithItem() {
        return em.createQuery(
                "select distinct o from Order o" +  // distinct 쓰면 DB에 distinct를 날려주고, 루트 엔티티가 중복일 경우에는 알아서 걸러서 컬렉션에 담아준다.
                " join fetch o.member m" +
                " join fetch o.delivery d" +
                " join fetch o.orderItems oi" +
                " join fetch oi.item i", Order.class)
                .getResultList();
    }



//    페치 조인으로 SQL이 1번만 실행됨
//    distinct 를 사용한 이유는 1대다 조인이 있으므로 데이터베이스 row가 증가한다. 그 결과 같은 order
//    엔티티의 조회 수도 증가하게 된다. JPA의 distinct는 SQL에 distinct를 추가하고, 더해서 같은 엔티티가
//    조회되면, 애플리케이션에서 중복을 걸러준다. 이 예에서 order가 컬렉션 페치 조인 때문에 중복 조회 되는
//    것을 막아준다.
//    단점
//    페이징 불가능
//    일대다 페치조인에서는 페이징을 하면 안 된다!
//    참고: 컬렉션 페치 조인을 사용하면 페이징이 불가능하다. 하이버네이트는 경고 로그를 남기면서 모든
//    데이터를 DB에서 읽어오고, 메모리에서 페이징 해버린다(매우 위험하다). 자세한 내용은 자바 ORM 표준
//    JPA 프로그래밍의 페치 조인 부분을 참고하자.
//    참고: 컬렉션 페치 조인은 1개만 사용할 수 있다. 컬렉션 둘 이상에 페치 조인을 사용하면 안된다. 데이터가
//    부정합하게 조회될 수 있다. 자세한 내용은 자바 ORM 표준 JPA 프로그래밍을 참고하자.

    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        return em.createQuery(
                " select  o from Order o " +
                        " join fetch o.member m" +
                        " join fetch o.delivery d ", Order.class
        )       .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    /**
     * Querydsl로 처리
     * 컴파일 시점에 오류가 다 잡힌다!(매우 큰 장점!)
     */
    public List<Order> findAll(OrderSearch orderSearch) {
        return query
                .select(order)
                .from(order)
                .join(order.member, member)
                .where(statusEq(orderSearch.getOrderStatus()), nameLike(orderSearch.getMemberName()))
                .limit(1000)
                .fetch();
        
    }

    private static BooleanExpression nameLike(String memberName) {
        if (!StringUtils.hasText(memberName)) {
            return null;
        }
        return member.name.like(memberName);
    }

    private BooleanExpression statusEq(OrderStatus statusCond) {
        if(statusCond == null) {
            return null;
        }
        return order.status.eq(statusCond);
    }



}
