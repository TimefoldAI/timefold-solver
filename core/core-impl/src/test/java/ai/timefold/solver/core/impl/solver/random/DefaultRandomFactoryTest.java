@Test
void createRandom_jdk() {
 DefaultRandomFactory factory = new DefaultRandomFactory(RandomType.JDK, 
null);
 Random random = factory.createRandom();
 assertThat(random).isInstanceOf(Random.class);
}
