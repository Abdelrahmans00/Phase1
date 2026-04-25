package com.example.phase1.service;
import com.example.phase1.entity.User;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class UserService {

    @PersistenceContext
    private EntityManager em;

    public void register(User user){
        em.persist(user);
    }

    public User login(String email, String password){
        try{
            return em.createQuery(
                    "select u from User u where u.email = :email AND u.password = :password",
                    User.class
            ).setParameter("email", email).setParameter("password", password).getSingleResult();
        } catch (Exception e) {
            return null ;
        }
    }

    public boolean emailExists(String email) {
        Long count = em.createQuery(
                        "select COUNT(u) from User u WHERE u.email = :email", Long.class
                )
                .setParameter("email", email)
                .getSingleResult();

        return count > 0;
    }

    public User findByEmail(String email){
        try{
            return em.createQuery(
                    "select u from User u where u.email = :email", User.class
            ).setParameter("email", email).getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public User updateProfile(String email, String name, String bio){
        try{
            User user = em.createQuery(
                    "select u from User u where u.email = :email",User.class
            ).setParameter("email", email).getSingleResult();
            user.setName(name);
            user.setBio(bio);
            return user;
        } catch (Exception e) {
            return null;
        }
    }
}
